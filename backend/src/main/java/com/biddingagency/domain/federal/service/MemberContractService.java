package com.biddingagency.domain.federal.service;

import com.biddingagency.domain.event.ContractEndApproachingEvent;
import com.biddingagency.domain.federal.dto.FederalContractResponses.ContractRow;
import com.biddingagency.domain.federal.dto.FederalContractResponses.LookupResponse;
import com.biddingagency.domain.federal.dto.FederalContractResponses.MemberContractRow;
import com.biddingagency.domain.federal.dto.FederalContractResponses.MemberContractsResponse;
import com.biddingagency.domain.federal.dto.MemberContractRequest;
import com.biddingagency.domain.federal.entity.ContractDateKind;
import com.biddingagency.domain.federal.entity.FederalContract;
import com.biddingagency.domain.federal.entity.MemberContract;
import com.biddingagency.domain.federal.entity.MemberContractSource;
import com.biddingagency.domain.federal.exception.FederalContractException;
import com.biddingagency.domain.federal.repository.FederalContractRepository;
import com.biddingagency.domain.federal.repository.MemberContractRepository;
import com.biddingagency.domain.notification.entity.NotificationLog;
import com.biddingagency.domain.notification.repository.NotificationLogRepository;
import com.biddingagency.domain.setting.entity.SettingKey;
import com.biddingagency.integration.usaspending.UsaSpendingClient;
import com.biddingagency.integration.usaspending.UsaSpendingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * 옵션 연장 알림의 회원 계약 (CR-142, BID-MKT-031, 032, 033).
 */
@Slf4j
@Service
public class MemberContractService {

    /** 알림 이력의 참조 종류. 알림 모듈이 같은 값으로 남긴다 */
    public static final String REFERENCE_TYPE = "MemberContract";
    private static final int DESCRIPTION_MAX = 500;

    private final MemberContractRepository memberContractRepository;
    private final FederalContractRepository contractRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final FederalContractQueryService queryService;
    private final UsaSpendingClient client;
    private final FederalSettings settings;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate tx;
    private final LocalDate dataFrom;

    public MemberContractService(MemberContractRepository memberContractRepository,
                                 FederalContractRepository contractRepository,
                                 NotificationLogRepository notificationLogRepository,
                                 FederalContractQueryService queryService,
                                 UsaSpendingClient client,
                                 FederalSettings settings,
                                 ApplicationEventPublisher eventPublisher,
                                 PlatformTransactionManager transactionManager,
                                 @Value("${app.federal-contract.data-from:2023-10-01}") LocalDate dataFrom) {
        this.memberContractRepository = memberContractRepository;
        this.contractRepository = contractRepository;
        this.notificationLogRepository = notificationLogRepository;
        this.queryService = queryService;
        this.client = client;
        this.settings = settings;
        this.eventPublisher = eventPublisher;
        this.tx = new TransactionTemplate(transactionManager);
        this.dataFrom = dataFrom;
    }

    /** 앞뒤 공백·하이픈·빈칸을 빼고 대문자로. 비면 400 */
    static String normalizePiid(String raw) {
        String piid = raw == null ? "" : raw.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
        if (piid.isEmpty()) throw FederalContractException.badRequest("CONTRACT_PIID_INVALID", "계약번호를 넣어 주세요");
        return piid;
    }

    /**
     * 계약번호로 찾기. 표에 없으면 받는 범위 안에서 USAspending 검색 1회, 옵션 끝날짜를 아직 받지 않았으면 상세 1회.
     * USAspending 이 실패하면 502, 화면은 직접 넣기로 안내한다.
     */
    public LookupResponse lookup(UUID memberId, String rawPiid) {
        String piid = normalizePiid(rawPiid);
        try {
            Optional<FederalContract> found = findStored(piid);
            if (found.isEmpty()) found = searchAndStore(piid);
            if (found.isPresent() && found.get().getPotentialEndDate() == null
                    && found.get().getDetailFetchedAt() == null) {
                found = Optional.of(fetchDetail(found.get()));
            }
            boolean registered = memberContractRepository.existsByMemberIdAndPiid(memberId, piid);
            return new LookupResponse(found.isPresent(), found.map(this::plainRow).orElse(null), registered);
        } catch (UsaSpendingException e) {
            log.warn("계약번호 찾기 중 USAspending 실패: piid={}, {}", piid, e.getMessage());
            throw FederalContractException.badGateway("USASPENDING_FAILED", "지금 공개 자료를 찾지 못했습니다");
        }
    }

    /** 같은 계약번호가 여럿이면(상위 계약이 다른 주문) 가장 최근에 바뀐 것 */
    private Optional<FederalContract> findStored(String piid) {
        return contractRepository.findByPiid(piid).stream()
                .max(Comparator.comparing(FederalContract::getSourceModifiedDate,
                        Comparator.nullsFirst(Comparator.naturalOrder())));
    }

    private Optional<FederalContract> searchAndStore(String piid) {
        List<FederalContract> rows = client.search(dataFrom, settings.today(), false, List.of(piid), 1).stream()
                .map(FederalContractRows::fromSearch)
                .flatMap(Optional::stream)
                .filter(c -> piid.equalsIgnoreCase(c.getPiid()))
                .toList();
        if (rows.isEmpty()) return Optional.empty();
        tx.executeWithoutResult(status -> {
            for (FederalContract c : rows) {
                contractRepository.findByAwardKey(c.getAwardKey())
                        .ifPresentOrElse(saved -> saved.overwrite(c), () -> contractRepository.save(c));
            }
        });
        return findStored(piid);
    }

    private FederalContract fetchDetail(FederalContract c) {
        Map<String, Object> detail = client.detail(c.getAwardKey());
        LocalDateTime now = settings.now();
        return tx.execute(status -> {
            FederalContract saved = contractRepository.findById(c.getId()).orElseThrow();
            saved.applyDetail(FederalContractRows.potentialEndDate(detail), FederalContractRows.solicitationNumber(detail),
                    FederalContractRows.detailRaw(detail), now);
            return saved;
        });
    }

    /**
     * 등록. 표에서 계약을 찾으면 공개 자료로 등록하고 본문 날짜는 쓰지 않는다(BIZ-056).
     * 못 찾으면 직접 넣은 계약이고 지금 끝날짜가 있어야 한다.
     */
    @Transactional
    public MemberContractRow register(UUID memberId, MemberContractRequest req) {
        String piid = normalizePiid(req.piid());
        if (memberContractRepository.existsByMemberIdAndPiid(memberId, piid)) {
            throw FederalContractException.conflict("MEMBER_CONTRACT_DUPLICATE", "이미 넣은 계약번호입니다");
        }
        String description = trimDescription(req.description());
        Optional<FederalContract> found = findStored(piid).filter(c -> c.getEndDate() != null);
        MemberContract saved;
        if (found.isPresent()) {
            FederalContract c = found.get();
            saved = memberContractRepository.save(MemberContract.builder()
                    .memberId(memberId).piid(piid).federalContractId(c.getId())
                    .source(MemberContractSource.PUBLIC)
                    .description(description != null ? description : trimDescription(c.getDescription()))
                    .endDate(c.getEndDate()).potentialEndDate(c.getPotentialEndDate())
                    .build());
        } else {
            validateDates(req.endDate(), req.potentialEndDate());
            saved = memberContractRepository.save(MemberContract.builder()
                    .memberId(memberId).piid(piid).source(MemberContractSource.MANUAL).description(description)
                    .endDate(req.endDate()).potentialEndDate(req.potentialEndDate())
                    .build());
        }
        return row(saved, settings.alertDays(), settings.today());
    }

    /** 직접 넣은 계약의 날짜와 설명 고치기. 공개 자료로 등록한 계약은 409 */
    @Transactional
    public MemberContractRow update(UUID memberId, UUID id, MemberContractRequest req) {
        MemberContract m = owned(memberId, id);
        if (m.getSource() == MemberContractSource.PUBLIC) {
            throw FederalContractException.conflict("MEMBER_CONTRACT_PUBLIC_DATES",
                    "공개 자료에서 찾은 계약의 날짜는 고칠 수 없습니다");
        }
        validateDates(req.endDate(), req.potentialEndDate());
        m.changeManual(req.endDate(), req.potentialEndDate(), trimDescription(req.description()));
        return row(m, settings.alertDays(), settings.today());
    }

    @Transactional
    public void delete(UUID memberId, UUID id) {
        memberContractRepository.delete(owned(memberId, id));
    }

    /** 내 계약 목록. 지금 끝날짜가 가까운 순 */
    @Transactional(readOnly = true)
    public MemberContractsResponse list(UUID memberId) {
        List<Integer> alertDays = settings.alertDays();
        LocalDate today = settings.today();
        List<MemberContractRow> rows = memberContractRepository.findByMemberId(memberId).stream()
                .sorted(Comparator.comparing(MemberContract::getEndDate).thenComparing(MemberContract::getPiid))
                .map(m -> row(m, alertDays, today))
                .toList();
        return new MemberContractsResponse(alertDays, today, rows, queryService.source());
    }

    /**
     * 지금 끝날짜나 옵션 끝날짜가 오늘부터 정확히 n일 뒤(n 은 알림 시점)인 회원 계약마다 알림 이벤트를 낸다.
     * 두 날짜가 같으면 옵션 끝날짜 알림만 낸다(BIZ-055). 한 번만 보내는 것은 알림 모듈의 멱등 키가 맡는다.
     *
     * @return 낸 이벤트 수
     */
    @Transactional(readOnly = true)
    public int publishDueAlerts() {
        List<Integer> alertDays = settings.alertDays();
        if (alertDays.isEmpty()) {
            log.info("계약 끝 알림이 꺼져 있어 보내지 않습니다 ({})", SettingKey.CONTRACT_END_ALERT_DAYS);
            return 0;
        }
        LocalDate today = settings.today();
        int published = 0;
        for (int days : alertDays) {
            LocalDate target = today.plusDays(days);
            for (MemberContract m : memberContractRepository.findEndingOn(target)) {
                for (ContractDateKind kind : dueKinds(m, target)) {
                    eventPublisher.publishEvent(new ContractEndApproachingEvent(m.getId(), m.getMemberId(),
                            m.getPiid(), m.getDescription(), kind, target, days));
                    published++;
                }
            }
        }
        log.info("계약 끝 알림 {}건 (오늘 {}, 시점 {})", published, today, alertDays);
        return published;
    }

    /** 그날 걸린 날짜 종류. 두 날짜가 같으면 옵션 끝날짜만 */
    static List<ContractDateKind> dueKinds(MemberContract m, LocalDate target) {
        List<ContractDateKind> kinds = new ArrayList<>();
        boolean same = Objects.equals(m.getEndDate(), m.getPotentialEndDate());
        if (target.equals(m.getEndDate()) && !same) kinds.add(ContractDateKind.CURRENT_END);
        if (target.equals(m.getPotentialEndDate())) kinds.add(ContractDateKind.POTENTIAL_END);
        return kinds;
    }

    /** 알림 모듈과 같은 멱등 키(BIZ-055) */
    public static String idempotencyKey(UUID memberContractId, ContractDateKind kind, LocalDate targetDate, int days) {
        return "CONTRACT_END_" + memberContractId + "_" + kind + "_" + targetDate + "_" + days;
    }

    private MemberContract owned(UUID memberId, UUID id) {
        return memberContractRepository.findByIdAndMemberId(id, memberId)
                .orElseThrow(() -> FederalContractException.notFound("MEMBER_CONTRACT_NOT_FOUND", "계약이 없습니다"));
    }

    private static void validateDates(LocalDate end, LocalDate potentialEnd) {
        if (end == null) {
            throw FederalContractException.badRequest("CONTRACT_END_DATE_REQUIRED", "지금 끝날짜를 넣어 주세요");
        }
        if (potentialEnd != null && potentialEnd.isBefore(end)) {
            throw FederalContractException.badRequest("CONTRACT_DATES_INVALID", "옵션 끝날짜는 지금 끝날짜보다 앞일 수 없습니다");
        }
    }

    private static String trimDescription(String s) {
        if (s == null || s.isBlank()) return null;
        String t = s.trim();
        return t.length() > DESCRIPTION_MAX ? t.substring(0, DESCRIPTION_MAX) : t;
    }

    private ContractRow plainRow(FederalContract c) {
        return queryService.row(c, null, List.of(), Map.of());
    }

    private MemberContractRow row(MemberContract m, List<Integer> alertDays, LocalDate today) {
        Map<String, Boolean> logged = new HashMap<>();
        LocalDateTime lastAlertedAt = null;
        if (m.getId() != null) {
            for (NotificationLog l : notificationLogRepository.findByReferenceTypeAndReferenceId(REFERENCE_TYPE, m.getId())) {
                if (l.getIdempotencyKey() != null) logged.put(l.getIdempotencyKey(), true);
                if (Boolean.TRUE.equals(l.getSuccess()) && l.getSentAt() != null
                        && (lastAlertedAt == null || l.getSentAt().isAfter(lastAlertedAt))) {
                    lastAlertedAt = l.getSentAt();
                }
            }
        }
        LocalDate nextOn = null;
        ContractDateKind nextKind = null;
        for (ContractDateKind kind : ContractDateKind.values()) {
            LocalDate target = kind == ContractDateKind.CURRENT_END ? m.getEndDate() : m.getPotentialEndDate();
            if (target == null) continue;
            for (int days : alertDays) {
                LocalDate on = target.minusDays(days);
                if (on.isBefore(today) || !dueKinds(m, target).contains(kind)) continue;
                if (logged.containsKey(idempotencyKey(m.getId(), kind, target, days))) continue;
                if (nextOn == null || on.isBefore(nextOn)) {
                    nextOn = on;
                    nextKind = kind;
                }
            }
        }
        ContractRow federal = m.getFederalContractId() == null ? null
                : contractRepository.findById(m.getFederalContractId()).map(this::plainRow).orElse(null);
        return new MemberContractRow(m.getId(), m.getPiid(), m.getDescription(), m.getSource(), m.getEndDate(),
                m.getPotentialEndDate(), daysBetween(today, m.getEndDate()), daysBetween(today, m.getPotentialEndDate()),
                nextOn, nextKind, m.getDatesChangedOn(), lastAlertedAt, federal);
    }

    private static Integer daysBetween(LocalDate today, LocalDate date) {
        return date == null ? null : (int) ChronoUnit.DAYS.between(today, date);
    }
}
