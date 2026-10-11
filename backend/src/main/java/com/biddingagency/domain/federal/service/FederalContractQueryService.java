package com.biddingagency.domain.federal.service;

import com.biddingagency.domain.federal.dto.FederalContractResponses.AwardRangeResponse;
import com.biddingagency.domain.federal.dto.FederalContractResponses.ContractRow;
import com.biddingagency.domain.federal.dto.FederalContractResponses.NaicsOption;
import com.biddingagency.domain.federal.dto.FederalContractResponses.NaicsResponse;
import com.biddingagency.domain.federal.dto.FederalContractResponses.RangeGroup;
import com.biddingagency.domain.federal.dto.FederalContractResponses.RangeRow;
import com.biddingagency.domain.federal.dto.FederalContractResponses.Source;
import com.biddingagency.domain.federal.entity.FederalContract;
import com.biddingagency.domain.federal.exception.FederalContractException;
import com.biddingagency.domain.federal.entity.FederalSyncStatus;
import com.biddingagency.domain.federal.repository.FederalContractRepository;
import com.biddingagency.domain.federal.repository.FederalContractSyncRunRepository;
import com.biddingagency.domain.notice.repository.NoticeRepository;
import com.biddingagency.domain.opportunity.dto.MarketSource;
import com.biddingagency.domain.opportunity.entity.OpportunityVisibility;
import com.biddingagency.domain.opportunity.repository.OpportunityRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 미 연방 계약 자료 조회 (CR-142, BID-MKT-034). federal_contracts 만 읽고 우리 공고와 섞지 않는다.
 * 재발주 예측 조회(BID-MKT-028~030)는 CR-147 에서 걷어냈다.
 * 원래 공고번호가 우리 공고에 있을 때 공고 상세로 보낼 공고문 id 만 찾아 붙인다.
 */
@Service
public class FederalContractQueryService {

    /** 업종 고르기 목록 건수(T3-2 C-4) */
    private static final int NAICS_OPTION_LIMIT = 50;
    /** 낙찰가 범위 근거 계약 줄 수(T3-2 C-4) */
    private static final int EVIDENCE_LIMIT = 20;

    private final FederalContractRepository contractRepository;
    private final FederalContractSyncRunRepository runRepository;
    private final OpportunityRepository opportunityRepository;
    private final NoticeRepository noticeRepository;
    private final FederalSettings settings;
    private final LocalDate dataFrom;
    private final String awardPageUrl;

    public FederalContractQueryService(FederalContractRepository contractRepository,
                                       FederalContractSyncRunRepository runRepository,
                                       OpportunityRepository opportunityRepository,
                                       NoticeRepository noticeRepository,
                                       FederalSettings settings,
                                       @Value("${app.federal-contract.data-from:2023-10-01}") LocalDate dataFrom,
                                       @Value("${app.federal-contract.award-page-url:https://www.usaspending.gov/award/}") String awardPageUrl) {
        this.contractRepository = contractRepository;
        this.runRepository = runRepository;
        this.opportunityRepository = opportunityRepository;
        this.noticeRepository = noticeRepository;
        this.settings = settings;
        this.dataFrom = dataFrom;
        this.awardPageUrl = awardPageUrl;
    }

    @Transactional(readOnly = true)
    public Source source() {
        return new Source(contractRepository.count(),
                runRepository.findFirstByStatusOrderByStartedAtDesc(FederalSyncStatus.SUCCEEDED)
                        .map(r -> r.getFinishedAt() == null ? null
                                : r.getFinishedAt().atZone(FederalSettings.ZONE).toOffsetDateTime())
                        .orElse(null),
                settings.lagDays(), dataFrom);
    }

    /**
     * 낙찰가 범위(BID-MKT-034, BIZ-054). 공고번호를 주면 그 공고의 업종을 쓴다.
     * 둘 다 없으면 400, 공고가 없으면 404, 공고에 업종 코드가 없으면 422.
     */
    @Transactional(readOnly = true)
    public AwardRangeResponse awardRange(String noticeNumber, String naics, String q) {
        if (noticeNumber == null && naics == null) {
            throw FederalContractException.badRequest("FEDERAL_RANGE_TARGET_REQUIRED", "noticeNumber 나 naics 가 필요합니다");
        }
        String code = naics;
        String noticeTitle = null;
        if (noticeNumber != null) {
            List<MarketSource> notices = opportunityRepository.findMarketSourcesBySolicitationNumbers(List.of(noticeNumber));
            if (notices.isEmpty()) {
                throw FederalContractException.notFound("FEDERAL_NOTICE_NOT_FOUND", "공고 " + noticeNumber + " 가 없습니다");
            }
            MarketSource latest = notices.stream()
                    .filter(s -> s.naicsCode() != null)
                    .max(Comparator.comparing(MarketSource::postedDate, Comparator.nullsFirst(Comparator.naturalOrder())))
                    .orElseThrow(() -> FederalContractException.unprocessable("FEDERAL_NOTICE_WITHOUT_NAICS",
                            "공고 " + noticeNumber + " 에 업종 코드가 없습니다"));
            code = latest.naicsCode();
            noticeTitle = latest.titleKo() != null && !latest.titleKo().isBlank() ? latest.titleKo() : latest.title();
        }
        final String naicsCode = code;
        String needle = q == null ? null : q.toLowerCase(Locale.ROOT);
        List<FederalContract> contracts = contractRepository.findByNaicsCode(naicsCode).stream()
                .filter(c -> needle == null
                        || (c.getDescription() != null && c.getDescription().toLowerCase(Locale.ROOT).contains(needle)))
                .toList();
        int minCount = settings.rangeMinCount();
        List<RangeRow> rows = List.of(
                range(RangeGroup.STANDALONE, contracts.stream().filter(c -> !c.getAwardType().isOrder()).toList(), minCount),
                range(RangeGroup.ORDER, contracts.stream().filter(c -> c.getAwardType().isOrder()).toList(), minCount));
        List<FederalContract> used = contracts.stream().filter(FederalContractQueryService::positive)
                .sorted(Comparator.comparing(FederalContract::getStartDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(FederalContract::getPiid))
                .limit(EVIDENCE_LIMIT)
                .toList();
        Map<String, UUID> notices = publicNotices(used.stream().map(FederalContract::getSolicitationNumber)
                .filter(Objects::nonNull).collect(Collectors.toSet()));
        List<ContractRow> evidence = used.stream().map(c -> row(c, notices)).toList();
        String naicsName = contracts.stream().map(FederalContract::getNaicsName).filter(Objects::nonNull).findFirst()
                .orElseGet(() -> contractRepository.findNaicsName(naicsCode).orElse(null));
        return new AwardRangeResponse(naicsCode, naicsName, noticeNumber, noticeTitle, rows, evidence, source());
    }

    @Transactional(readOnly = true)
    public NaicsResponse naics(String q) {
        String needle = q == null || q.isBlank() ? null : q.trim().toLowerCase(Locale.ROOT);
        List<NaicsOption> items = contractRepository.countByNaics(needle, PageRequest.of(0, NAICS_OPTION_LIMIT))
                .stream()
                .map(r -> new NaicsOption((String) r[0], (String) r[1], ((Number) r[2]).longValue()))
                .toList();
        return new NaicsResponse(items, source());
    }

    ContractRow row(FederalContract c, Map<String, UUID> notices) {
        return new ContractRow(c.getId(), c.getPiid(), c.getParentPiid(), c.getAwardType(), c.getDescription(),
                c.getRecipientName(), c.getAmountUsd(), c.getNaicsCode(), c.getNaicsName(), c.getStartDate(),
                c.getEndDate(), c.getPotentialEndDate(), c.getSolicitationNumber(),
                c.getSolicitationNumber() == null ? null : notices.get(c.getSolicitationNumber().trim()),
                awardPageUrl + c.getAwardKey());
    }

    /** 공고번호마다 공개 한글 공고문 id. 공고번호가 같은 원본이 여럿이면 공개 공고문이 있는 것 아무거나 */
    Map<String, UUID> publicNotices(Collection<String> solicitationNumbers) {
        Set<String> sols = solicitationNumbers.stream().map(String::trim).filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        if (sols.isEmpty()) return Map.of();
        List<MarketSource> sources = opportunityRepository.findMarketSourcesBySolicitationNumbers(sols);
        if (sources.isEmpty()) return Map.of();
        Map<UUID, UUID> noticeByOpportunity = new HashMap<>();
        for (Object[] r : noticeRepository.findOpportunityAndNoticeIds(OpportunityVisibility.VISIBLE)) {
            noticeByOpportunity.put((UUID) r[0], (UUID) r[1]);
        }
        Map<String, UUID> out = new HashMap<>();
        for (MarketSource s : sources) {
            UUID notice = noticeByOpportunity.get(s.id());
            if (notice != null && s.solicitationNumber() != null) out.put(s.solicitationNumber().trim(), notice);
        }
        return out;
    }

    static RangeRow range(RangeGroup group, List<FederalContract> contracts, int minCount) {
        List<BigDecimal> amounts = contracts.stream().filter(FederalContractQueryService::positive)
                .map(FederalContract::getAmountUsd).sorted().toList();
        int count = amounts.size();
        int excluded = contracts.size() - count;
        if (count < minCount || count == 0) {
            return new RangeRow(group, count, excluded, minCount, null, null, null, null, null);
        }
        return new RangeRow(group, count, excluded, minCount, amounts.get(0), percentile(amounts, 0.25),
                percentile(amounts, 0.50), percentile(amounts, 0.75), amounts.get(count - 1));
    }

    /** 작은 순으로 놓은 금액의 선형 보간 백분위수 */
    static BigDecimal percentile(List<BigDecimal> sorted, double p) {
        double rank = p * (sorted.size() - 1);
        int lo = (int) Math.floor(rank);
        int hi = (int) Math.ceil(rank);
        BigDecimal low = sorted.get(lo);
        BigDecimal frac = BigDecimal.valueOf(rank - lo);
        return low.add(sorted.get(hi).subtract(low).multiply(frac)).setScale(2, RoundingMode.HALF_UP);
    }

    private static boolean positive(FederalContract c) {
        return c.getAmountUsd() != null && c.getAmountUsd().signum() > 0;
    }
}
