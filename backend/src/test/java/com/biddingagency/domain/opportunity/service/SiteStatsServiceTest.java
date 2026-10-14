package com.biddingagency.domain.opportunity.service;

import com.biddingagency.domain.federal.repository.FederalContractRepository;
import com.biddingagency.domain.notice.repository.NoticeRepository;
import com.biddingagency.domain.notice.service.NoticeService;
import com.biddingagency.domain.opportunity.dto.MarketSource;
import com.biddingagency.domain.opportunity.repository.OpportunityRepository;
import com.biddingagency.domain.opportunity.service.MarketDataService.CollectedFilter;
import com.biddingagency.domain.setting.service.SystemSettingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * TC-SITE-013 ~ TC-SITE-016 (CR-162): 첫 화면 사이트 자료 규모 한 줄의 숫자.
 */
@ExtendWith(MockitoExtension.class)
class SiteStatsServiceTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    /** 시험 기준 시각 2026-10-15 10:00 KST */
    private static final Clock NOW = Clock.fixed(Instant.parse("2026-10-15T01:00:00Z"), KST);
    private static final LocalDateTime NOW_KST = LocalDateTime.now(NOW);

    @Mock
    private OpportunityRepository opportunityRepository;
    @Mock
    private FederalContractRepository federalContractRepository;
    @Mock
    private NoticeRepository noticeRepository;
    @Mock
    private SystemSettingService settingService;
    @Mock
    private NoticeService noticeService;

    private SiteStatsService service() {
        return new SiteStatsService(opportunityRepository, federalContractRepository, NOW);
    }

    private static MarketSource source(String sol, String type, LocalDateTime posted, LocalDateTime deadline) {
        UUID id = UUID.randomUUID();
        return new MarketSource(id, id.toString().replace("-", ""), sol, "title " + id, null, type, null,
                posted, deadline, null, null, null, null, null, null, (String) null);
    }

    private void given모은공고(MarketSource... sources) {
        given(opportunityRepository.findMarketSources()).willReturn(List.of(sources));
    }

    /** 낙찰 공지 1, 마감 지난 공고 1, 마감 전 공고번호 C 원본과 정정 2판 */
    private void given마감섞인공고() {
        given모은공고(
                source("AW1", "Award Notice", NOW_KST.minusDays(5), NOW_KST.plusDays(3)),
                source("PAST", "Solicitation", NOW_KST.minusDays(9), NOW_KST.minusDays(1)),
                source("C", "Solicitation", NOW_KST.minusDays(4), NOW_KST.plusDays(6)),
                source("C", "Solicitation", NOW_KST.minusDays(2), NOW_KST.plusDays(8)));
    }

    // TC-SITE-013
    @Test
    @DisplayName("같은공고번호여러판_모은공고한건")
    void 같은공고번호여러판_모은공고한건() {
        given모은공고(
                source("A", "Solicitation", NOW_KST.minusDays(3), NOW_KST.plusDays(5)),
                source("A", "Solicitation", NOW_KST.minusDays(1), NOW_KST.plusDays(5)),
                source("B", "Solicitation", NOW_KST.minusDays(2), NOW_KST.plusDays(5)),
                source(null, "Solicitation", NOW_KST.minusDays(2), NOW_KST.plusDays(5)));

        assertThat(service().stats().notices()).isEqualTo(3);
    }

    // TC-SITE-014
    @Test
    @DisplayName("낙찰공지와마감지난공고_마감전에서뺌")
    void 낙찰공지와마감지난공고_마감전에서뺌() {
        given마감섞인공고();

        assertThat(service().stats().openNotices()).isEqualTo(1);
    }

    // TC-SITE-015
    @Test
    @DisplayName("미연방계약_행수그대로")
    void 미연방계약_행수그대로() {
        given모은공고();
        given(federalContractRepository.count()).willReturn(3L);

        assertThat(service().stats().federalContracts()).isEqualTo(3);
    }

    // TC-SITE-016
    @Test
    @DisplayName("마감전공고수_첫화면건수와같음")
    void 마감전공고수_첫화면건수와같음() {
        given마감섞인공고();
        MarketRowAssembler rowAssembler = new MarketRowAssembler(opportunityRepository, noticeRepository);
        ReflectionTestUtils.setField(rowAssembler, "publicOpportunityUrl", "https://sam.gov/opp/{noticeId}/view");
        MarketDataService market = new MarketDataService(opportunityRepository, rowAssembler,
                new NaicsTopLevelKoMapper(), settingService, noticeService, NOW);

        long homeTotal = market.collected(
                new CollectedFilter(null, null, null, null, true, true, null, null, null), 0, 10).totalElements();

        assertThat(service().stats().openNotices()).isEqualTo(homeTotal);
    }
}
