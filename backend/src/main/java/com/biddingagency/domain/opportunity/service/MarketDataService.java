package com.biddingagency.domain.opportunity.service;

import com.biddingagency.domain.notice.entity.Notice;
import com.biddingagency.domain.notice.service.NoticeService;
import com.biddingagency.domain.opportunity.MarketGroup;
import com.biddingagency.domain.opportunity.NoticeTypeTranslator;
import com.biddingagency.domain.opportunity.dto.MarketResponses.AmendmentGroup;
import com.biddingagency.domain.opportunity.dto.MarketResponses.AmendmentsResponse;
import com.biddingagency.domain.opportunity.dto.MarketResponses.AwardSummary;
import com.biddingagency.domain.opportunity.dto.MarketResponses.AwardsResponse;
import com.biddingagency.domain.opportunity.dto.MarketResponses.CollectedResponse;
import com.biddingagency.domain.opportunity.dto.MarketResponses.MonthCountItem;
import com.biddingagency.domain.opportunity.dto.MarketResponses.MonthStat;
import com.biddingagency.domain.opportunity.dto.MarketResponses.MonthlyResponse;
import com.biddingagency.domain.opportunity.dto.MarketResponses.NaicsResponse;
import com.biddingagency.domain.opportunity.dto.MarketResponses.SampleResponse;
import com.biddingagency.domain.opportunity.dto.MarketResponses.SectorStat;
import com.biddingagency.domain.opportunity.dto.MarketResponses.SubCodeCount;
import com.biddingagency.domain.opportunity.dto.MarketResponses.TypeCount;
import com.biddingagency.domain.opportunity.dto.MarketRowDto;
import com.biddingagency.domain.opportunity.dto.MarketSource;
import com.biddingagency.domain.opportunity.dto.OpportunityDto;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.repository.OpportunityRepository;
import com.biddingagency.domain.setting.entity.SettingKey;
import com.biddingagency.domain.setting.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 메뉴 속 화면 2묶음: 모은 공고 목록과 집계 (CR-135, BID-MKT-001~006).
 *
 * <p>관리자가 공개한 공고문만이 아니라 모은 원본 공고 전부(지운 것 제외)를 읽는다. 읽기만 한다.
 * 모은 공고는 수백 건이라 한 번에 읽어 메모리에서 거르고 센다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarketDataService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private static final Comparator<MarketSource> LATEST_FIRST = Comparator
            .comparing(MarketSource::postedDate, Comparator.nullsFirst(Comparator.naturalOrder()))
            .reversed()
            // 같은 시각이면 DB 의 id 순서(바이트 순)를 따른다. UUID.compareTo 는 부호 있는 비교라 DB 와 순서가 다르다
            .thenComparing(s -> s.id().toString());

    private static final Comparator<MarketSource> DEADLINE_FIRST = Comparator
            .comparing(MarketSource::responseDeadline, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(LATEST_FIRST);

    public enum AwardSort { LATEST, AMOUNT }

    public enum CollectedSort { LATEST, DEADLINE }

    /**
     * 모은 공고 목록 거르기 (T3-2 C-2 collected).
     *
     * @param latestOnly    같은 공고번호는 마지막 판만 (업종별 발주의 최근 공고, 마감 전 공고)
     * @param q             한글 제목, 원문 제목, 공고번호에서 찾는 검색어 (CR-155)
     * @param sort          null 이면 게시일 최신순 (CR-155)
     * @param dueWithinDays 마감이 지금부터 이 날 수 안인 것만 (CR-155)
     */
    public record CollectedFilter(MarketGroup group, String type, YearMonth month, String sector,
                                  boolean openOnly, boolean latestOnly, String q, CollectedSort sort,
                                  Integer dueWithinDays) {
    }

    private final OpportunityRepository opportunityRepository;
    private final MarketRowAssembler rowAssembler;
    private final NaicsTopLevelKoMapper naicsMapper;
    private final SystemSettingService settingService;
    private final NoticeService noticeService;
    private final Clock clock;

    /**
     * 모은 공고 목록 (BID-MKT-001, 005, 006).
     * 마감 전 공고 화면과 첫 화면도 쓴다(BID-BROWSE-001, 005).
     * total 과 자료 기간은 묶음·달·업종 거르기까지, 유형별 건수는 유형을 뺀 모든 거르기를 건 결과다.
     */
    public CollectedResponse collected(CollectedFilter f, int page, int size) {
        List<MarketSource> all = opportunityRepository.findMarketSources();
        List<MarketSource> base = all.stream()
                .filter(s -> f.group() == null || s.group() == f.group())
                .filter(s -> f.month() == null
                        || (s.postedDate() != null && YearMonth.from(s.postedDate()).equals(f.month())))
                .filter(s -> f.sector() == null || f.sector().equals(naicsMapper.sectorOf(s.naicsCode())))
                .toList();

        LocalDateTime now = nowKst();
        List<MarketSource> narrowed = base.stream()
                .filter(s -> !f.openOnly() || MarketRowAssembler.isOpen(s, now))
                .filter(s -> f.dueWithinDays() == null || dueWithin(s, now, f.dueWithinDays()))
                .toList();
        if (f.latestOnly()) narrowed = latestPerSolicitation(narrowed);
        if (f.q() != null) narrowed = narrowed.stream().filter(s -> matches(s, f.q())).toList();

        List<TypeCount> typeCounts = typeCounts(narrowed);
        List<MarketSource> filtered = narrowed.stream()
                .filter(s -> f.type() == null || f.type().equals(s.type()))
                .sorted(f.sort() == CollectedSort.DEADLINE ? DEADLINE_FIRST : LATEST_FIRST)
                .toList();

        MarketRowAssembler.Context ctx = rowAssembler.context(all);
        List<MarketRowDto> content = slice(filtered, page, size).stream().map(s -> rowAssembler.toRow(s, ctx)).toList();
        return new CollectedResponse(content, page, size, filtered.size(), totalPages(filtered.size(), size),
                base.size(), firstPosted(base), lastPosted(base), typeCounts);
    }

    /**
     * 변경 공고 (BID-MKT-002). 같은 공고번호로 두 번 이상 올라온 공고 묶음, 낙찰 공지는 세지 않는다(BIZ-026).
     * solicitationNumber 를 주면 그 공고번호 묶음만 남긴다(사전 공고의 "본 공고 나옴" 에서 온다).
     */
    public AmendmentsResponse amendments(String solicitationNumber, boolean openOnly, int page, int size) {
        List<MarketSource> all = opportunityRepository.findMarketSources();
        MarketRowAssembler.Context ctx = rowAssembler.context(all);
        LocalDateTime now = nowKst();

        Map<String, List<MarketSource>> bySol = all.stream()
                .filter(s -> s.solKey() != null && s.group() != MarketGroup.AWARD)
                .collect(Collectors.groupingBy(MarketSource::solKey));

        List<AmendmentGroup> groups = new ArrayList<>();
        List<MarketSource> grouped = new ArrayList<>();
        for (Map.Entry<String, List<MarketSource>> e : bySol.entrySet()) {
            if (e.getValue().size() < 2) continue;
            List<MarketSource> versions = e.getValue().stream()
                    .sorted(LATEST_FIRST.reversed())
                    .toList();
            grouped.addAll(versions);
            groups.add(amendmentGroup(e.getKey(), versions, ctx, now));
        }
        groups.sort(Comparator.comparing(AmendmentGroup::lastPostedDate,
                Comparator.nullsFirst(Comparator.naturalOrder())).reversed()
                .thenComparing(AmendmentGroup::solicitationNumber));

        long openCount = groups.stream().filter(AmendmentGroup::open).count();
        List<AmendmentGroup> filtered = groups.stream()
                .filter(g -> solicitationNumber == null || g.solicitationNumber().equals(solicitationNumber))
                .filter(g -> !openOnly || g.open())
                .toList();
        return new AmendmentsResponse(slice(filtered, page, size), page, size, filtered.size(),
                totalPages(filtered.size(), size), groups.size(), openCount, grouped.size(),
                firstPosted(grouped), lastPosted(grouped));
    }

    /**
     * 공고문 예시 (BID-MKT-003, POL-015). 설정이 비었거나 UUID 가 아니거나 공개 공고문이 아니면 빈 결과.
     */
    public Optional<SampleResponse> sample() {
        return settingService.findValue(SettingKey.NOTICE_SAMPLE_ID)
                .flatMap(this::parseUuid)
                .flatMap(noticeService::findVisibleById)
                .map(this::sampleOf);
    }

    private SampleResponse sampleOf(Notice notice) {
        Opportunity opp = notice.getOpportunity();
        MarketSource source = MarketRowAssembler.sourceOf(opp);
        List<MarketSource> versions = new ArrayList<>(List.of(source));
        if (source.solKey() != null) {
            opportunityRepository.findMarketSourcesBySolicitationNumbers(Set.of(source.solKey())).stream()
                    .filter(s -> s.group() != MarketGroup.AWARD)
                    .forEach(versions::add);
        }
        return new SampleResponse(OpportunityDto.fromNotice(notice), opp.getTitle(),
                rowAssembler.samUrl(opp.getNoticeId()), firstPosted(versions));
    }

    /**
     * 최근 낙찰 (BID-MKT-004). 요약은 달 거르기를 건 결과, 달별 건수와 자료 기간은 낙찰 공지 전부.
     * 금액은 낙찰 금액 숫자 칸(CR-136)을 쓰고, 비었으면 금액 없음으로 센다(BIZ-027).
     */
    public AwardsResponse awards(YearMonth month, AwardSort sort, int page, int size) {
        List<MarketSource> all = opportunityRepository.findMarketSources();
        List<MarketSource> awards = all.stream().filter(s -> s.group() == MarketGroup.AWARD).toList();
        MarketRowAssembler.Context ctx = rowAssembler.context(all);

        Map<YearMonth, Long> byMonth = awards.stream()
                .filter(s -> s.postedDate() != null)
                .collect(Collectors.groupingBy(s -> YearMonth.from(s.postedDate()), TreeMap::new, Collectors.counting()));
        List<MonthCountItem> monthCounts = byMonth.entrySet().stream()
                .map(e -> new MonthCountItem(e.getKey().toString(), e.getValue()))
                .toList();

        List<MarketSource> filtered = awards.stream()
                .filter(s -> month == null || (s.postedDate() != null && YearMonth.from(s.postedDate()).equals(month)))
                .toList();
        Map<UUID, BigDecimal> amounts = new HashMap<>();
        for (MarketSource s : filtered) {
            BigDecimal amount = s.awardAmountUsd();
            if (amount != null) amounts.put(s.id(), amount);
        }

        Comparator<MarketSource> order = sort == AwardSort.AMOUNT
                ? Comparator.<MarketSource, BigDecimal>comparing(s -> amounts.get(s.id()),
                        Comparator.nullsFirst(Comparator.naturalOrder())).reversed().thenComparing(LATEST_FIRST)
                : LATEST_FIRST;
        List<MarketRowDto> rows = filtered.stream().sorted(order).map(s -> rowAssembler.toRow(s, ctx)).toList();

        AwardSummary summary = new AwardSummary(
                filtered.size(),
                amounts.size(),
                amounts.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add),
                amounts.values().stream().max(Comparator.naturalOrder()).orElse(null),
                rows.stream().filter(r -> r.awardeeName() != null).count());

        return new AwardsResponse(slice(rows, page, size), page, size, rows.size(), totalPages(rows.size(), size),
                summary, monthCounts, awards.size(), firstPosted(awards), lastPosted(awards));
    }

    /**
     * 월별 동향 (BID-MKT-005). 첫 게시 달부터 이번 달(KST)까지, 모은 공고가 없는 달도 0 으로 넣는다.
     */
    public MonthlyResponse monthly() {
        List<MarketSource> all = opportunityRepository.findMarketSources().stream()
                .filter(s -> s.postedDate() != null)
                .toList();
        YearMonth current = YearMonth.from(nowKst());
        if (all.isEmpty()) {
            return new MonthlyResponse(List.of(), List.of(), current.toString(), 0, null, null);
        }

        Map<YearMonth, Map<MarketGroup, Long>> counts = all.stream()
                .collect(Collectors.groupingBy(s -> YearMonth.from(s.postedDate()),
                        Collectors.groupingBy(MarketSource::group, Collectors.counting())));
        YearMonth first = counts.keySet().stream().min(Comparator.naturalOrder()).orElse(current);

        List<MonthStat> months = new ArrayList<>();
        List<String> emptyMonths = new ArrayList<>();
        for (YearMonth m = first; !m.isAfter(current); m = m.plusMonths(1)) {
            Map<MarketGroup, Long> c = counts.getOrDefault(m, Map.of());
            long bid = c.getOrDefault(MarketGroup.BID, 0L);
            long pre = c.getOrDefault(MarketGroup.PRE, 0L);
            long award = c.getOrDefault(MarketGroup.AWARD, 0L);
            long other = c.getOrDefault(MarketGroup.OTHER, 0L);
            long total = bid + pre + award + other;
            if (total == 0) emptyMonths.add(m.toString());
            months.add(new MonthStat(m.toString(), total, bid, pre, award, other));
        }
        return new MonthlyResponse(months, emptyMonths, current.toString(), all.size(),
                firstPosted(all), lastPosted(all));
    }

    /**
     * 업종별 발주 (BID-MKT-006). 원본 공고의 업종 코드 대분류로 묶는다. 코드 없는 공고는 noneCount 로만 센다.
     */
    public NaicsResponse naics() {
        List<MarketSource> all = opportunityRepository.findMarketSources();
        Map<String, List<MarketSource>> bySector = all.stream()
                .collect(Collectors.groupingBy(s -> naicsMapper.sectorOf(s.naicsCode())));
        long none = bySector.getOrDefault(NaicsTopLevelKoMapper.NONE, List.of()).size();

        List<SectorStat> sectors = bySector.entrySet().stream()
                .filter(e -> !NaicsTopLevelKoMapper.NONE.equals(e.getKey()))
                .map(e -> new SectorStat(e.getKey(), naicsMapper.sectorKo(e.getKey()), e.getValue().size(),
                        subCodes(e.getValue())))
                .sorted(Comparator.comparingLong(SectorStat::count).reversed().thenComparing(SectorStat::code))
                .toList();
        return new NaicsResponse(sectors, none, all.size() - none, all.size(), firstPosted(all), lastPosted(all));
    }

    private AmendmentGroup amendmentGroup(String sol, List<MarketSource> versions, MarketRowAssembler.Context ctx,
                                          LocalDateTime now) {
        MarketSource first = versions.get(0);
        MarketSource last = versions.get(versions.size() - 1);
        long distinctDeadlines = versions.stream().map(MarketSource::responseDeadline).filter(Objects::nonNull)
                .distinct().count();
        boolean hasAward = ctx.bySol().getOrDefault(sol, List.of()).stream()
                .anyMatch(s -> s.group() == MarketGroup.AWARD);
        List<MarketRowDto> rows = versions.stream().map(s -> rowAssembler.toRow(s, ctx)).toList();
        MarketRowDto lastRow = rows.get(rows.size() - 1);
        return new AmendmentGroup(sol, last.title(), last.titleKo(), versions.size(),
                first.postedDate() == null ? null : first.postedDate().toLocalDate(),
                lastRow.postedDate(), lastRow.responseDeadline(), distinctDeadlines > 1, hasAward,
                MarketRowAssembler.isOpen(last, now), rows);
    }

    private List<TypeCount> typeCounts(List<MarketSource> sources) {
        Map<String, Long> byType = sources.stream()
                .filter(s -> s.type() != null)
                .collect(Collectors.groupingBy(MarketSource::type, LinkedHashMap::new, Collectors.counting()));
        Map<String, String> typeKo = sources.stream()
                .filter(s -> s.type() != null && s.typeKo() != null)
                .collect(Collectors.toMap(MarketSource::type, MarketSource::typeKo, (a, b) -> a));
        return byType.entrySet().stream()
                .map(e -> new TypeCount(e.getKey(),
                        typeKo.getOrDefault(e.getKey(), NoticeTypeTranslator.toKorean(e.getKey())), e.getValue()))
                .sorted(Comparator.comparingLong(TypeCount::count).reversed().thenComparing(TypeCount::type))
                .toList();
    }

    private static List<SubCodeCount> subCodes(List<MarketSource> sources) {
        return sources.stream()
                .collect(Collectors.groupingBy(MarketSource::naicsCode, Collectors.counting()))
                .entrySet().stream()
                .map(e -> new SubCodeCount(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(SubCodeCount::count).reversed().thenComparing(SubCodeCount::code))
                .toList();
    }

    private static boolean dueWithin(MarketSource s, LocalDateTime now, int days) {
        return MarketRowAssembler.isOpen(s, now) && !s.responseDeadline().isAfter(now.plusDays(days));
    }

    private static boolean matches(MarketSource s, String q) {
        String needle = q.toLowerCase(Locale.ROOT);
        return containsIgnoreCase(s.titleKo(), needle) || containsIgnoreCase(s.title(), needle)
                || containsIgnoreCase(s.solicitationNumber(), needle);
    }

    private static boolean containsIgnoreCase(String value, String lowerNeedle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(lowerNeedle);
    }

    /** 같은 공고번호는 게시가 가장 늦은 판 하나만 남긴다. 사이트 자료 규모도 같은 계산을 쓴다(CR-162) */
    static List<MarketSource> latestPerSolicitation(List<MarketSource> sources) {
        Map<String, MarketSource> latest = sources.stream()
                .filter(s -> s.solKey() != null)
                .collect(Collectors.toMap(MarketSource::solKey, Function.identity(),
                        (a, b) -> LATEST_FIRST.compare(a, b) <= 0 ? a : b));
        List<MarketSource> out = new ArrayList<>(latest.values());
        sources.stream().filter(s -> s.solKey() == null).forEach(out::add);
        return out;
    }

    private Optional<UUID> parseUuid(String value) {
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            log.warn("공고문 예시 설정값이 UUID 가 아니라 빈 결과로 봅니다: key={}, value={}",
                    SettingKey.NOTICE_SAMPLE_ID, value);
            return Optional.empty();
        }
    }

    private LocalDateTime nowKst() {
        return LocalDateTime.now(clock.withZone(KST));
    }

    private static LocalDate firstPosted(List<MarketSource> sources) {
        return sources.stream().map(MarketSource::postedDate).filter(Objects::nonNull)
                .min(Comparator.naturalOrder()).map(LocalDateTime::toLocalDate).orElse(null);
    }

    private static LocalDate lastPosted(List<MarketSource> sources) {
        return sources.stream().map(MarketSource::postedDate).filter(Objects::nonNull)
                .max(Comparator.naturalOrder()).map(LocalDateTime::toLocalDate).orElse(null);
    }

    private static <T> List<T> slice(List<T> list, int page, int size) {
        int from = Math.min(page * size, list.size());
        int to = Math.min(from + size, list.size());
        return list.subList(from, to);
    }

    private static int totalPages(int count, int size) {
        return count == 0 ? 0 : (count + size - 1) / size;
    }
}
