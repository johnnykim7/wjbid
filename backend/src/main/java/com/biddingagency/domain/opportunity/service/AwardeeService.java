package com.biddingagency.domain.opportunity.service;

import com.biddingagency.domain.opportunity.MarketGroup;
import com.biddingagency.domain.opportunity.dto.AwardeeResponses.AwardRow;
import com.biddingagency.domain.opportunity.dto.AwardeeResponses.Awardee;
import com.biddingagency.domain.opportunity.dto.AwardeeResponses.AwardeesResponse;
import com.biddingagency.domain.opportunity.dto.MarketSource;
import com.biddingagency.domain.opportunity.repository.OpportunityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 낙찰 업체 (CR-136, BID-MKT-009). 업체 이름이 있는 낙찰 공지를 업체별로 묶는다(BIZ-029).
 * 숫자는 저장된 낙찰 공지로만 센다(BIZ-028).
 */
@Service
@RequiredArgsConstructor
public class AwardeeService {

    public enum AwardeeSort { COUNT, AMOUNT }

    private static final String UEI_KEY = "UEI:";
    private static final String NAME_KEY = "NAME:";

    private static final Comparator<MarketSource> LATEST_FIRST = Comparator
            .comparing(MarketSource::postedDate, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(MarketSource::id);
    private static final Comparator<Awardee> BY_NAME = Comparator.comparing(Awardee::name);
    private static final Comparator<Awardee> BY_AMOUNT_DESC = Comparator.comparing(Awardee::amountSum,
            Comparator.nullsLast(Comparator.reverseOrder()));
    private static final Comparator<Awardee> BY_COUNT_DESC = Comparator.comparingInt(Awardee::awardCount).reversed();

    private final OpportunityRepository opportunityRepository;
    private final MarketRowAssembler rowAssembler;

    /**
     * @param q 업체 이름 일부(대소문자 무시). 묶음 안 어느 공지의 이름에든 들어 있으면 남긴다
     */
    @Transactional(readOnly = true)
    public AwardeesResponse awardees(String q, AwardeeSort sort) {
        List<MarketSource> all = opportunityRepository.findMarketSources();
        List<MarketSource> awards = all.stream().filter(s -> s.group() == MarketGroup.AWARD).toList();
        List<MarketSource> named = awards.stream().filter(s -> hasText(s.awardeeName())).toList();
        MarketRowAssembler.Context ctx = rowAssembler.context(all);

        Map<String, List<MarketSource>> groups = named.stream().collect(Collectors.groupingBy(AwardeeService::key));
        List<Awardee> sorted = groups.values().stream()
                .map(group -> awardee(group.stream().sorted(LATEST_FIRST).toList(), ctx))
                .sorted(sort == AwardeeSort.AMOUNT
                        ? BY_AMOUNT_DESC.thenComparing(BY_COUNT_DESC).thenComparing(BY_NAME)
                        : BY_COUNT_DESC.thenComparing(BY_AMOUNT_DESC).thenComparing(BY_NAME))
                .toList();

        // 순위는 찾기 전 전체 순서로 매겨, 찾은 업체가 1위로 보이지 않게 한다
        String needle = hasText(q) ? q.trim().toUpperCase(Locale.ROOT) : null;
        List<Awardee> out = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            Awardee a = sorted.get(i);
            if (needle == null || groups.get(a.key()).stream()
                    .anyMatch(s -> s.awardeeName().toUpperCase(Locale.ROOT).contains(needle))) {
                out.add(a.ranked(i + 1));
            }
        }

        return new AwardeesResponse(posted(awards, Comparator.naturalOrder()), posted(awards, Comparator.reverseOrder()),
                awards.size(), named.size(), out);
    }

    /** 같은 업체 판정 (BIZ-029). UEI 가 있으면 UEI, 없으면 정규화한 이름 */
    static String key(MarketSource s) {
        return hasText(s.awardeeUei())
                ? UEI_KEY + s.awardeeUei().trim().toUpperCase(Locale.ROOT)
                : NAME_KEY + normalizeName(s.awardeeName());
    }

    static String normalizeName(String name) {
        return name.toUpperCase(Locale.ROOT).replace(".", "").replace(",", "").trim();
    }

    private Awardee awardee(List<MarketSource> latestFirst, MarketRowAssembler.Context ctx) {
        MarketSource latest = latestFirst.get(0);
        List<BigDecimal> amounts = latestFirst.stream().map(MarketSource::awardAmountUsd)
                .filter(Objects::nonNull).toList();
        BigDecimal sum = amounts.isEmpty() ? null : amounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        String uei = latestFirst.stream().map(MarketSource::awardeeUei).filter(AwardeeService::hasText)
                .findFirst().orElse(null);
        List<AwardRow> rows = latestFirst.stream()
                .map(s -> new AwardRow(rowAssembler.toRow(s, ctx), s.awardAmountUsd()))
                .toList();
        return new Awardee(key(latest), 0, latest.awardeeName(), uei, latestFirst.size(), sum, amounts.size(),
                latest.postedDate() == null ? null : latest.postedDate().toLocalDate(), rows);
    }

    private static LocalDate posted(List<MarketSource> sources, Comparator<LocalDateTime> pick) {
        return sources.stream().map(MarketSource::postedDate).filter(Objects::nonNull)
                .min(pick).map(LocalDateTime::toLocalDate).orElse(null);
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
