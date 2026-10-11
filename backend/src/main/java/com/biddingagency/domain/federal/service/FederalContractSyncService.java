package com.biddingagency.domain.federal.service;

import com.biddingagency.domain.federal.dto.FederalContractResponses.SyncRunRow;
import com.biddingagency.domain.federal.entity.FederalAwardType;
import com.biddingagency.domain.federal.entity.FederalContract;
import com.biddingagency.domain.federal.entity.FederalContractSyncRun;
import com.biddingagency.domain.federal.entity.FederalSyncMode;
import com.biddingagency.domain.federal.entity.FederalSyncStatus;
import com.biddingagency.domain.federal.entity.MemberContract;
import com.biddingagency.domain.federal.entity.MemberContractSource;
import com.biddingagency.domain.federal.repository.FederalContractRepository;
import com.biddingagency.domain.federal.repository.FederalContractSyncRunRepository;
import com.biddingagency.domain.federal.repository.MemberContractRepository;
import com.biddingagency.integration.usaspending.UsaSpendingClient;
import com.biddingagency.integration.usaspending.UsaSpendingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 미 연방 계약 받기 (CR-142, BID-MKT-025, BID-MKT-026, BID-MKT-027).
 *
 * 한 회차는 검색(전체 또는 바뀐 것), 옵션 끝날짜 상세, 회원 계약 날짜 따라가기 순으로 돈다.
 * 쪽마다 따로 저장하므로 중간에 멈춰도 그때까지 받은 행은 남는다(BIZ-053).
 */
@Slf4j
@Service
public class FederalContractSyncService {

    static final String STOPPED_BY_RESTART = "앱 재기동으로 멈춤";
    /** 상위 계약이 없는 계약. 옵션 끝날짜 상세를 받는 대상(BID-MKT-026) */
    static final Set<FederalAwardType> STANDALONE_TYPES =
            EnumSet.of(FederalAwardType.PURCHASE_ORDER, FederalAwardType.DEFINITIVE_CONTRACT);
    private static final int ERROR_MESSAGE_MAX = 2000;
    private static final int MAX_RUNS = 100;

    private final FederalContractRepository contractRepository;
    private final FederalContractSyncRunRepository runRepository;
    private final MemberContractRepository memberContractRepository;
    private final UsaSpendingClient client;
    private final FederalSettings settings;
    private final TransactionTemplate tx;
    private final LocalDate dataFrom;
    private final int calendarMonths;
    private final int detailLookbackDays;
    private final int maxPages;
    /** 이 앱에서 돌고 있는 회차가 있는지. 없는데 DB 에 RUNNING 이 남아 있으면 앱이 멈춘 흔적이다 */
    private final AtomicBoolean running = new AtomicBoolean(false);

    public FederalContractSyncService(FederalContractRepository contractRepository,
                                      FederalContractSyncRunRepository runRepository,
                                      MemberContractRepository memberContractRepository,
                                      UsaSpendingClient client,
                                      FederalSettings settings,
                                      PlatformTransactionManager transactionManager,
                                      @Value("${app.federal-contract.data-from:2023-10-01}") LocalDate dataFrom,
                                      @Value("${app.federal-contract.calendar-months:12}") int calendarMonths,
                                      @Value("${app.federal-contract.detail-lookback-days:120}") int detailLookbackDays,
                                      @Value("${app.federal-contract.max-pages:1000}") int maxPages) {
        this.contractRepository = contractRepository;
        this.runRepository = runRepository;
        this.memberContractRepository = memberContractRepository;
        this.client = client;
        this.settings = settings;
        this.tx = new TransactionTemplate(transactionManager);
        this.dataFrom = dataFrom;
        this.calendarMonths = calendarMonths;
        this.detailLookbackDays = detailLookbackDays;
        this.maxPages = maxPages;
    }

    private static final class Counter {
        int searchCalls;
        int detailCalls;
        int inserted;
        int updated;
    }

    /**
     * 회차를 만든다. 매일 받기인데 성공한 회차가 없으면 전체 받기로 만든다.
     * 돌고 있는 회차가 있으면 SyncAlreadyRunningException. 만든 뒤에는 반드시 execute 를 부른다.
     */
    public FederalContractSyncRun start(FederalSyncMode mode, String triggeredBy) {
        if (!running.compareAndSet(false, true)) throw new SyncAlreadyRunningException();
        try {
            return tx.execute(status -> {
                LocalDateTime now = settings.now();
                for (FederalContractSyncRun stale : runRepository.findByStatus(FederalSyncStatus.RUNNING)) {
                    stale.fail(STOPPED_BY_RESTART, now);
                }
                FederalSyncMode actual = mode;
                LocalDate since = null;
                if (mode == FederalSyncMode.DAILY) {
                    Optional<FederalContractSyncRun> last =
                            runRepository.findFirstByStatusOrderByStartedAtDesc(FederalSyncStatus.SUCCEEDED);
                    if (last.isPresent()) {
                        since = last.get().getStartedAt().toLocalDate().minusDays(1);
                    } else {
                        actual = FederalSyncMode.FULL;
                    }
                }
                return runRepository.save(FederalContractSyncRun.start(actual, since, now, triggeredBy));
            });
        } catch (RuntimeException e) {
            running.set(false);
            throw e;
        }
    }

    /** start 로 만든 회차를 끝까지 돌린다 */
    public void execute(UUID runId) {
        Counter counter = new Counter();
        try {
            FederalContractSyncRun run = runRepository.findById(runId).orElseThrow();
            fetchSearch(run, counter);
            fetchDetails(runId, counter);
            int followed = followMemberContracts();
            finish(runId, counter, null);
            log.info("미 연방 계약 받기 성공: mode={}, 검색 {}회, 상세 {}회, 넣음 {}, 덮음 {}, 회원 계약 날짜 바뀜 {}",
                    run.getMode(), counter.searchCalls, counter.detailCalls, counter.inserted, counter.updated, followed);
        } catch (Exception e) {
            log.error("미 연방 계약 받기 실패: run={}", runId, e);
            finish(runId, counter, message(e));
        } finally {
            running.set(false);
        }
    }

    /** 최근 받기 회차, 최근 시작순(T3-2 B-5) */
    @Transactional(readOnly = true)
    public List<SyncRunRow> recentRuns(int size) {
        return runRepository.findAllByOrderByStartedAtDesc(PageRequest.of(0, Math.min(Math.max(size, 1), MAX_RUNS)))
                .stream()
                .map(r -> new SyncRunRow(r.getId(), r.getMode(), r.getStatus(), r.getModifiedSince(), r.getStartedAt(),
                        r.getFinishedAt(), r.getSearchCalls(), r.getDetailCalls(), r.getInsertedCount(),
                        r.getUpdatedCount(), r.getErrorMessage(), r.getTriggeredBy()))
                .toList();
    }

    private void fetchSearch(FederalContractSyncRun run, Counter counter) {
        boolean daily = run.getMode() == FederalSyncMode.DAILY;
        LocalDate from = daily ? run.getModifiedSince() : dataFrom;
        LocalDate to = settings.today();
        for (int page = 1; ; page++) {
            if (page > maxPages) {
                throw new IllegalStateException("검색 쪽이 " + maxPages + "쪽을 넘었습니다");
            }
            List<Map<String, Object>> rows = client.search(from, to, daily, null, page);
            counter.searchCalls++;
            tx.executeWithoutResult(status -> upsert(rows, counter));
            saveProgress(run.getId(), counter);
            if (rows.size() < UsaSpendingClient.PAGE_SIZE) return;
        }
    }

    /** 계약 고유 키로 넣거나 덮어쓴다(BIZ-052). 받은 적 없는 계약은 지우지 않는다 */
    private void upsert(List<Map<String, Object>> rows, Counter counter) {
        Map<String, FederalContract> incoming = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            FederalContractRows.fromSearch(row).ifPresentOrElse(c -> incoming.put(c.getAwardKey(), c),
                    () -> log.warn("미 연방 계약 줄을 읽지 못해 건너뜁니다: {}", row.get("generated_internal_id")));
        }
        if (incoming.isEmpty()) return;
        Map<String, FederalContract> existing = contractRepository.findByAwardKeyIn(incoming.keySet()).stream()
                .collect(Collectors.toMap(FederalContract::getAwardKey, Function.identity()));
        List<FederalContract> created = new ArrayList<>();
        for (FederalContract c : incoming.values()) {
            FederalContract saved = existing.get(c.getAwardKey());
            if (saved != null) {
                saved.overwrite(c);
                counter.updated++;
            } else {
                created.add(c);
                counter.inserted++;
            }
        }
        contractRepository.saveAll(created);
    }

    /** 옵션 끝날짜와 원래 공고번호(BID-MKT-026). 4xx(429 제외)는 그 계약만 건너뛴다(BIZ-053) */
    private void fetchDetails(UUID runId, Counter counter) {
        LocalDate today = settings.today();
        LocalDate from = today.minusDays(detailLookbackDays);
        LocalDate to = YearMonth.from(today).plusMonths(calendarMonths - 1L).atEndOfMonth();
        Map<UUID, FederalContract> targets = new LinkedHashMap<>();
        contractRepository.findDetailTargets(STANDALONE_TYPES, from, to).forEach(c -> targets.put(c.getId(), c));
        contractRepository.findMemberDetailTargets().forEach(c -> targets.putIfAbsent(c.getId(), c));
        for (FederalContract c : targets.values()) {
            Map<String, Object> detail;
            counter.detailCalls++;
            try {
                detail = client.detail(c.getAwardKey());
            } catch (UsaSpendingException e) {
                if (!e.isClientError()) throw e;
                log.warn("미 연방 계약 상세를 건너뜁니다: {} ({})", c.getAwardKey(), e.getMessage());
                continue;
            }
            LocalDateTime now = settings.now();
            tx.executeWithoutResult(status -> contractRepository.findById(c.getId()).ifPresent(saved ->
                    saved.applyDetail(FederalContractRows.potentialEndDate(detail),
                            FederalContractRows.solicitationNumber(detail),
                            FederalContractRows.detailRaw(detail), now)));
            saveProgress(runId, counter);
        }
    }

    /** 공개 자료로 등록한 회원 계약은 미 연방 계약 행의 날짜를 따라간다(BIZ-056) */
    private int followMemberContracts() {
        Integer changed = tx.execute(status -> {
            List<MemberContract> members =
                    memberContractRepository.findBySourceAndFederalContractIdIsNotNull(MemberContractSource.PUBLIC);
            if (members.isEmpty()) return 0;
            Map<UUID, FederalContract> contracts = new HashMap<>();
            contractRepository.findAllById(members.stream().map(MemberContract::getFederalContractId).toList())
                    .forEach(c -> contracts.put(c.getId(), c));
            LocalDate today = settings.today();
            int n = 0;
            for (MemberContract m : members) {
                FederalContract c = contracts.get(m.getFederalContractId());
                if (c != null && m.followPublic(c.getEndDate(), c.getPotentialEndDate(), today)) n++;
            }
            return n;
        });
        return changed == null ? 0 : changed;
    }

    private void saveProgress(UUID runId, Counter counter) {
        tx.executeWithoutResult(status -> runRepository.findById(runId).ifPresent(run ->
                run.record(counter.searchCalls, counter.detailCalls, counter.inserted, counter.updated)));
    }

    private void finish(UUID runId, Counter counter, String error) {
        tx.executeWithoutResult(status -> runRepository.findById(runId).ifPresent(run -> {
            run.record(counter.searchCalls, counter.detailCalls, counter.inserted, counter.updated);
            if (error == null) run.succeed(settings.now());
            else run.fail(error, settings.now());
        }));
    }

    private static String message(Exception e) {
        String m = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        return m.length() > ERROR_MESSAGE_MAX ? m.substring(0, ERROR_MESSAGE_MAX) : m;
    }
}
