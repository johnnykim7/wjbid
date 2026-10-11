package com.biddingagency.domain.base.service;

import com.biddingagency.domain.base.entity.MilitaryBase;
import com.biddingagency.domain.base.repository.MilitaryBaseRepository;
import com.biddingagency.domain.base.repository.OpportunityTitleRepository;
import com.biddingagency.domain.base.repository.OpportunityTitleView;
import com.biddingagency.domain.opportunity.dto.MarketRowDto;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.repository.OpportunityRepository;
import com.biddingagency.domain.opportunity.service.AwardInfo;
import com.biddingagency.domain.opportunity.service.MarketRowAssembler;
import com.biddingagency.dto.BaseRequest;
import com.biddingagency.dto.BaseResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 기지 관리와 기지별 집계 (CR-136, BID-MKT-010, BID-MKT-011)
 *
 * 숫자는 저장된 공고로만 센다(BIZ-028). 공고는 공고번호로 묶어 세고, 한 공고번호의 판 중 어느 제목이든
 * 기지에 걸리면 그 기지 공고로 본다. 낙찰 공지는 따로 센다.
 */
@Service
@RequiredArgsConstructor
public class MilitaryBaseService {

    private static final BigDecimal LAT_LIMIT = BigDecimal.valueOf(90);
    private static final BigDecimal LNG_LIMIT = BigDecimal.valueOf(180);
    private static final int NAME_MAX = 100;
    private static final int REGION_MAX = 100;
    private static final int TERMS_MAX = 1000;
    /** 마감 시각은 한국 시각으로 저장돼 있어 지금도 한국 시각으로 본다 */
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final MilitaryBaseRepository baseRepository;
    private final OpportunityTitleRepository titleRepository;
    private final OpportunityRepository opportunityRepository;
    private final MarketRowAssembler rowAssembler;
    private final Clock clock;

    // ── 관리자 ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<BaseResponses.AdminBase> listForAdmin() {
        List<MilitaryBase> bases = baseRepository.findAllByOrderBySortOrderAscNameKoAsc();
        Index idx = index(bases);
        return bases.stream()
                .map(b -> new BaseResponses.AdminBase(b.getId(), b.getNameKo(), b.getNameEn(), b.terms(),
                        b.getRegion(), b.getLatitude(), b.getLongitude(), b.getSortOrder(),
                        idx.keysOf(b.getId()).size()))
                .toList();
    }

    @Transactional
    public MilitaryBase create(BaseRequest req) {
        Valid v = validate(req);
        if (baseRepository.existsByNameEnIgnoreCase(v.nameEn())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "같은 영문 이름의 기지가 있습니다");
        }
        return baseRepository.save(MilitaryBase.builder()
                .nameKo(v.nameKo()).nameEn(v.nameEn()).matchTerms(v.matchTerms()).region(v.region())
                .latitude(req.latitude()).longitude(req.longitude()).sortOrder(v.sortOrder())
                .build());
    }

    @Transactional
    public MilitaryBase update(UUID id, BaseRequest req) {
        MilitaryBase base = find(id);
        Valid v = validate(req);
        if (baseRepository.existsByNameEnIgnoreCaseAndIdNot(v.nameEn(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "같은 영문 이름의 기지가 있습니다");
        }
        base.update(v.nameKo(), v.nameEn(), v.matchTerms(), v.region(), req.latitude(), req.longitude(), v.sortOrder());
        return base;
    }

    @Transactional
    public void delete(UUID id) {
        baseRepository.delete(find(id));
    }

    /** 어느 기지에도 걸리지 않은 공고(낙찰 공지 제외). 공고번호마다 가장 최근 판, 최근 게시순 */
    @Transactional(readOnly = true)
    public BaseResponses.Unmatched unmatched() {
        Index idx = index(baseRepository.findAllByOrderBySortOrderAscNameKoAsc());
        List<BaseResponses.UnmatchedRow> rows = idx.latestByKey.entrySet().stream()
                .filter(e -> idx.basesByKey.getOrDefault(e.getKey(), Set.of()).isEmpty())
                .map(Map.Entry::getValue)
                .sorted(LATEST_FIRST)
                .map(o -> new BaseResponses.UnmatchedRow(o.id(), o.noticeId(), o.solicitationNumber(), o.title(),
                        o.titleKo(), o.postedDate() != null ? o.postedDate().toLocalDate() : null))
                .toList();
        return new BaseResponses.Unmatched(rows.size(), rows);
    }

    // ── 기지별 지도 ─────────────────────────────────────────

    @Transactional(readOnly = true)
    public BaseResponses.BaseStats stats() {
        List<MilitaryBase> bases = baseRepository.findAllByOrderBySortOrderAscNameKoAsc();
        Index idx = index(bases);
        LocalDateTime now = LocalDateTime.now(clock.withZone(KST));
        List<BaseResponses.BaseStat> stats = new ArrayList<>();
        for (MilitaryBase b : bases) {
            Set<String> keys = idx.keysOf(b.getId());
            int open = (int) keys.stream().map(idx.latestByKey::get).filter(o -> isOpen(o, now)).count();
            stats.add(new BaseResponses.BaseStat(b.getId(), b.getNameKo(), b.getNameEn(), b.getRegion(),
                    b.getLatitude(), b.getLongitude(), keys.size(),
                    idx.awardsByBase.getOrDefault(b.getId(), 0), open));
        }
        int unmatched = (int) idx.latestByKey.keySet().stream()
                .filter(k -> idx.basesByKey.getOrDefault(k, Set.of()).isEmpty()).count();
        return new BaseResponses.BaseStats(idx.dataFrom, idx.dataTo, idx.latestByKey.size(), unmatched, stats);
    }

    /** 그 기지 공고. 공고번호마다 가장 최근 판, 최근 게시순으로 size 건(낙찰 공지 제외) */
    @Transactional(readOnly = true)
    public List<MarketRowDto> recentOpportunities(UUID baseId, int size) {
        find(baseId);
        Index idx = index(baseRepository.findAllByOrderBySortOrderAscNameKoAsc());
        List<UUID> ids = idx.keysOf(baseId).stream()
                .map(idx.latestByKey::get)
                .sorted(LATEST_FIRST)
                .limit(size)
                .map(OpportunityTitleView::id)
                .toList();
        Map<UUID, Opportunity> byId = opportunityRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Opportunity::getId, Function.identity()));
        return rowAssembler.toRows(ids.stream().map(byId::get).filter(Objects::nonNull).toList());
    }

    // ── 내부 ────────────────────────────────────────────────

    private static final Comparator<OpportunityTitleView> LATEST_FIRST = Comparator.comparing(
            OpportunityTitleView::postedDate, Comparator.nullsLast(Comparator.reverseOrder()));

    private static boolean isOpen(OpportunityTitleView o, LocalDateTime now) {
        return o != null && o.responseDeadline() != null && o.responseDeadline().isAfter(now);
    }

    private MilitaryBase find(UUID id) {
        return baseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "기지를 찾을 수 없습니다"));
    }

    /** 공고와 기지를 한 번에 맞춰 본 결과 */
    private static final class Index {
        /** 공고번호 묶음 키마다 가장 최근 판(낙찰 공지 제외) */
        final Map<String, OpportunityTitleView> latestByKey = new LinkedHashMap<>();
        /** 묶음 키마다 걸린 기지 */
        final Map<String, Set<UUID>> basesByKey = new HashMap<>();
        /** 기지마다 낙찰 공지 수 */
        final Map<UUID, Integer> awardsByBase = new HashMap<>();
        LocalDate dataFrom;
        LocalDate dataTo;

        Set<String> keysOf(UUID baseId) {
            Set<String> keys = new LinkedHashSet<>();
            basesByKey.forEach((k, ids) -> {
                if (ids.contains(baseId)) keys.add(k);
            });
            return keys;
        }
    }

    private Index index(List<MilitaryBase> bases) {
        BaseMatcher matcher = BaseMatcher.of(bases);
        List<OpportunityTitleView> all = titleRepository.findAllLive();
        Index idx = new Index();
        List<OpportunityTitleView> awards = new ArrayList<>();
        for (OpportunityTitleView o : all) {
            if (o.postedDate() != null) {
                LocalDate d = o.postedDate().toLocalDate();
                if (idx.dataFrom == null || d.isBefore(idx.dataFrom)) idx.dataFrom = d;
                if (idx.dataTo == null || d.isAfter(idx.dataTo)) idx.dataTo = d;
            }
            if (AwardInfo.isAwardNotice(o.type())) {
                awards.add(o);
                continue;
            }
            String key = o.numberKey();
            idx.latestByKey.merge(key, o, (a, b) -> LATEST_FIRST.compare(a, b) <= 0 ? a : b);
            idx.basesByKey.computeIfAbsent(key, k -> new LinkedHashSet<>()).addAll(matcher.match(o.title()));
        }
        for (OpportunityTitleView a : awards) {
            Set<UUID> hit = matcher.match(a.title());
            if (hit.isEmpty() && a.solicitationNumber() != null && !a.solicitationNumber().isBlank()) {
                hit = idx.basesByKey.getOrDefault(a.numberKey(), Set.of());
            }
            hit.forEach(id -> idx.awardsByBase.merge(id, 1, Integer::sum));
        }
        return idx;
    }

    private record Valid(String nameKo, String nameEn, String matchTerms, String region, int sortOrder) {
    }

    private Valid validate(BaseRequest req) {
        if (req == null) throw bad("본문이 비어 있습니다");
        String nameKo = trimToNull(req.nameKo());
        String nameEn = trimToNull(req.nameEn());
        if (nameKo == null || nameEn == null) throw bad("한글 이름과 영문 이름을 넣어 주세요");
        if (nameKo.length() > NAME_MAX || nameEn.length() > NAME_MAX) throw bad("이름이 너무 깁니다");
        List<String> terms = req.matchTerms() == null ? List.of() : req.matchTerms().stream()
                .filter(Objects::nonNull)
                .map(t -> t.replace(MilitaryBase.TERM_SEPARATOR, " ").trim())
                .filter(t -> !t.isEmpty())
                .distinct()
                .toList();
        if (terms.isEmpty()) throw bad("찾을 낱말을 하나 이상 넣어 주세요");
        String joined = String.join(MilitaryBase.TERM_SEPARATOR, terms);
        if (joined.length() > TERMS_MAX) throw bad("찾을 낱말이 너무 깁니다");
        String region = trimToNull(req.region());
        if (region != null && region.length() > REGION_MAX) throw bad("지역 이름이 너무 깁니다");
        BigDecimal lat = req.latitude();
        BigDecimal lng = req.longitude();
        if ((lat == null) != (lng == null)) throw bad("위도와 경도는 둘 다 넣거나 둘 다 비워 주세요");
        if (lat != null && (lat.abs().compareTo(LAT_LIMIT) > 0 || lng.abs().compareTo(LNG_LIMIT) > 0)) {
            throw bad("위도는 -90~90, 경도는 -180~180 이어야 합니다");
        }
        return new Valid(nameKo, nameEn, joined, region, req.sortOrder() != null ? req.sortOrder() : 0);
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
