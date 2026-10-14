package com.biddingagency.domain.opportunity.service;

import com.biddingagency.domain.federal.repository.FederalContractRepository;
import com.biddingagency.domain.opportunity.dto.MarketResponses.SiteStats;
import com.biddingagency.domain.opportunity.dto.MarketSource;
import com.biddingagency.domain.opportunity.repository.OpportunityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 사이트 자료 규모 (BID-SITE-003, T3-2 C-3a). 첫 화면 대표 문장 아래 한 줄이 쓴다.
 * 숫자는 조회 때마다 센다(BIZ-082). 마감 전 판정과 같은 공고번호 하나만 남기는 계산은 모은 공고 목록의 것을
 * 그대로 써 첫 화면 마감 전 공고 건수와 어긋나지 않게 한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SiteStatsService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final OpportunityRepository opportunityRepository;
    private final FederalContractRepository federalContractRepository;
    private final Clock clock;

    public SiteStats stats() {
        List<MarketSource> all = opportunityRepository.findMarketSources();
        LocalDateTime now = LocalDateTime.now(clock.withZone(KST));
        List<MarketSource> open = all.stream().filter(s -> MarketRowAssembler.isOpen(s, now)).toList();
        return new SiteStats(
                federalContractRepository.count(),
                MarketDataService.latestPerSolicitation(all).size(),
                MarketDataService.latestPerSolicitation(open).size());
    }
}
