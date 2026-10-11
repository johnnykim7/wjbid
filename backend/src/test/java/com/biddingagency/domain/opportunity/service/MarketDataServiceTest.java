package com.biddingagency.domain.opportunity.service;

import com.biddingagency.domain.notice.repository.NoticeRepository;
import com.biddingagency.domain.notice.service.NoticeService;
import com.biddingagency.domain.opportunity.MarketGroup;
import com.biddingagency.domain.opportunity.dto.MarketResponses.AmendmentGroup;
import com.biddingagency.domain.opportunity.dto.MarketResponses.AwardSummary;
import com.biddingagency.domain.opportunity.dto.MarketResponses.MonthStat;
import com.biddingagency.domain.opportunity.dto.MarketResponses.MonthlyResponse;
import com.biddingagency.domain.opportunity.dto.MarketResponses.NaicsResponse;
import com.biddingagency.domain.opportunity.dto.MarketResponses.SectorStat;
import com.biddingagency.domain.opportunity.dto.MarketResponses.TypeCount;
import com.biddingagency.domain.opportunity.dto.MarketRowDto;
import com.biddingagency.domain.opportunity.dto.MarketSource;
import com.biddingagency.domain.opportunity.repository.OpportunityRepository;
import com.biddingagency.domain.opportunity.service.MarketDataService.AwardSort;
import com.biddingagency.domain.setting.entity.SettingKey;
import com.biddingagency.domain.setting.service.SystemSettingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.BDDMockito.given;

/**
 * TC-MKT-001 ~ TC-MKT-017 (CR-135): 메뉴 속 화면 2묶음의 모은 공고 목록과 집계.
 * TC-MKT-018(지운 공고 빠짐)은 조회 쿼리가 거르므로 OpportunityRepositoryTest 에 있다.
 */
@ExtendWith(MockitoExtension.class)
class MarketDataServiceTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    /** 시험 기준 시각 2026-10-11 10:00 KST */
    private static final Clock NOW = Clock.fixed(Instant.parse("2026-10-11T01:00:00Z"), KST);
    private static final LocalDateTime NOW_KST = LocalDateTime.now(NOW);

    @Mock
    private OpportunityRepository opportunityRepository;
    @Mock
    private NoticeRepository noticeRepository;
    @Mock
    private SystemSettingService settingService;
    @Mock
    private NoticeService noticeService;

    private MarketRowAssembler rowAssembler;

    @BeforeEach
    void setUp() {
        rowAssembler = new MarketRowAssembler(opportunityRepository, noticeRepository);
        ReflectionTestUtils.setField(rowAssembler, "publicOpportunityUrl", "https://sam.gov/opp/{noticeId}/view");
    }

    private MarketDataService service(Clock clock) {
        return new MarketDataService(opportunityRepository, rowAssembler, new NaicsTopLevelKoMapper(),
                settingService, noticeService, clock);
    }

    private MarketDataService service() {
        return service(NOW);
    }

    private static MarketSource source(String sol, String type, LocalDateTime posted, LocalDateTime deadline) {
        return source(sol, type, posted, deadline, null, null);
    }

    private static MarketSource source(String sol, String type, LocalDateTime posted, LocalDateTime deadline,
                                       BigDecimal amountUsd, String naicsCode) {
        UUID id = UUID.randomUUID();
        return new MarketSource(id, id.toString().replace("-", ""), sol, "title " + id, null, type, null,
                posted, deadline, null, amountUsd, null, null, naicsCode);
    }

    private void given모은공고(MarketSource... sources) {
        given(opportunityRepository.findMarketSources()).willReturn(List.of(sources));
    }

    // TC-MKT-001
    @Test
    @DisplayName("사전묶음_모은공고조회_사전공고와사전수요조사만")
    void 사전묶음_모은공고조회_사전공고와사전수요조사만() {
        given모은공고(
                source("S1", "Presolicitation", NOW_KST.minusDays(3), null),
                source("S2", "Sources Sought", NOW_KST.minusDays(2), null),
                source("S3", "Solicitation", NOW_KST.minusDays(1), null));

        List<TypeCount> counts = service().collected(MarketGroup.PRE, null, null, null, false, false, 0, 20)
                .typeCounts();

        assertThat(counts).extracting(TypeCount::type, TypeCount::count)
                .containsExactlyInAnyOrder(tuple("Presolicitation", 1L), tuple("Sources Sought", 1L));
    }

    // TC-MKT-002
    @Test
    @DisplayName("기한남은것만_모은공고조회_지난기한과빈기한제외")
    void 기한남은것만_모은공고조회_지난기한과빈기한제외() {
        MarketSource tomorrow = source("S1", "Presolicitation", NOW_KST.minusDays(3), NOW_KST.plusDays(1));
        given모은공고(
                tomorrow,
                source("S2", "Presolicitation", NOW_KST.minusDays(2), NOW_KST.minusDays(1)),
                source("S3", "Presolicitation", NOW_KST.minusDays(1), null));

        List<MarketRowDto> rows = service().collected(null, null, null, null, true, false, 0, 20).content();

        assertThat(rows).extracting(MarketRowDto::id).containsExactly(tomorrow.id());
    }

    // TC-MKT-003
    @Test
    @DisplayName("같은공고번호입찰있음_사전공고조회_본공고나옴참")
    void 같은공고번호입찰있음_사전공고조회_본공고나옴참() {
        given모은공고(
                source("W1", "Sources Sought", NOW_KST.minusDays(10), null),
                source("W1", "Solicitation", NOW_KST.minusDays(2), NOW_KST.plusDays(20)));

        List<MarketRowDto> rows = service().collected(MarketGroup.PRE, null, null, null, false, false, 0, 20)
                .content();

        assertThat(rows).singleElement().extracting(MarketRowDto::hasMainNotice).isEqualTo(true);
    }

    // TC-MKT-004
    @Test
    @DisplayName("같은공고번호두판_변경공고조회_한묶음두판")
    void 같은공고번호두판_변경공고조회_한묶음두판() {
        MarketSource first = source("W1", "Solicitation", NOW_KST.minusDays(10), NOW_KST.plusDays(5));
        MarketSource second = source("W1", "Solicitation", NOW_KST.minusDays(3), NOW_KST.plusDays(9));
        given모은공고(second, first);

        List<AmendmentGroup> groups = service().amendments(null, false, 0, 20).content();

        assertThat(groups).singleElement()
                .extracting(g -> g.versions().stream().map(MarketRowDto::id).toList())
                .isEqualTo(List.of(first.id(), second.id()));
    }

    // TC-MKT-005
    @Test
    @DisplayName("입찰한판과낙찰공지_변경공고조회_묶지않음")
    void 입찰한판과낙찰공지_변경공고조회_묶지않음() {
        given모은공고(
                source("W1", "Solicitation", NOW_KST.minusDays(30), NOW_KST.minusDays(10)),
                source("W1", "Award Notice", NOW_KST.minusDays(2), null));

        List<AmendmentGroup> groups = service().amendments(null, false, 0, 20).content();

        assertThat(groups).isEmpty();
    }

    // TC-MKT-006
    @Test
    @DisplayName("판마다마감일다름_변경공고조회_마감일바뀜참")
    void 판마다마감일다름_변경공고조회_마감일바뀜참() {
        given모은공고(
                source("W1", "Solicitation", LocalDateTime.of(2026, 9, 20, 9, 0), LocalDateTime.of(2026, 10, 1, 17, 0)),
                source("W1", "Solicitation", LocalDateTime.of(2026, 9, 25, 9, 0), LocalDateTime.of(2026, 10, 8, 17, 0)));

        List<AmendmentGroup> groups = service().amendments(null, false, 0, 20).content();

        assertThat(groups).singleElement().extracting(AmendmentGroup::deadlineChanged).isEqualTo(true);
    }

    // TC-MKT-007
    @Test
    @DisplayName("마지막판마감지남_진행중만_제외됨")
    void 마지막판마감지남_진행중만_제외됨() {
        given모은공고(
                source("W1", "Solicitation", NOW_KST.minusDays(20), NOW_KST.plusDays(3)),
                source("W1", "Solicitation", NOW_KST.minusDays(5), NOW_KST.minusDays(1)));

        List<AmendmentGroup> groups = service().amendments(null, true, 0, 20).content();

        assertThat(groups).isEmpty();
    }

    // TC-MKT-008
    @Test
    @DisplayName("예시설정없음_공고문예시_빈결과")
    void 예시설정없음_공고문예시_빈결과() {
        given(settingService.findValue(SettingKey.NOTICE_SAMPLE_ID)).willReturn(Optional.empty());

        assertThat(service().sample()).isEmpty();
    }

    // TC-MKT-009: 숨김 공고문은 노출 공고문 찾기에서 나오지 않는다
    @Test
    @DisplayName("예시공고문숨김_공고문예시_빈결과")
    void 예시공고문숨김_공고문예시_빈결과() {
        UUID hidden = UUID.randomUUID();
        given(settingService.findValue(SettingKey.NOTICE_SAMPLE_ID)).willReturn(Optional.of(hidden.toString()));
        given(noticeService.findVisibleById(hidden)).willReturn(Optional.empty());

        assertThat(service().sample()).isEmpty();
    }

    // TC-MKT-010
    @Test
    @DisplayName("예시값UUID아님_공고문예시_빈결과")
    void 예시값UUID아님_공고문예시_빈결과() {
        given(settingService.findValue(SettingKey.NOTICE_SAMPLE_ID)).willReturn(Optional.of("abc"));

        assertThat(service().sample()).isEmpty();
    }

    // TC-MKT-011: 금액은 낙찰 금액 숫자 칸(CR-136)을 쓴다
    @Test
    @DisplayName("금액칸_낙찰요약_숫자로합산")
    void 금액칸_낙찰요약_숫자로합산() {
        given모은공고(
                source("A1", "Award Notice", NOW_KST.minusDays(3), null, new BigDecimal("1234.50"), null),
                source("A2", "Award Notice", NOW_KST.minusDays(2), null, new BigDecimal("100"), null),
                source("A3", "Award Notice", NOW_KST.minusDays(1), null, null, null));

        AwardSummary summary = service().awards(null, AwardSort.LATEST, 0, 20).summary();

        assertThat(List.of(summary.amountSum().toPlainString(), summary.amountMax().toPlainString(),
                summary.amountKnownCount(), summary.total()))
                .containsExactly("1334.50", "1234.50", 2L, 3L);
    }

    // TC-MKT-012
    @Test
    @DisplayName("금액순정렬_낙찰목록_금액없는공지맨뒤")
    void 금액순정렬_낙찰목록_금액없는공지맨뒤() {
        MarketSource a100 = source("A1", "Award Notice", NOW_KST.minusDays(3), null, new BigDecimal("100"), null);
        MarketSource none = source("A2", "Award Notice", NOW_KST.minusDays(2), null, null, null);
        MarketSource a300 = source("A3", "Award Notice", NOW_KST.minusDays(1), null, new BigDecimal("300"), null);
        given모은공고(a100, none, a300);

        List<MarketRowDto> rows = service().awards(null, AwardSort.AMOUNT, 0, 20).content();

        assertThat(rows).extracting(MarketRowDto::id).containsExactly(a300.id(), a100.id(), none.id());
    }

    // TC-MKT-013: 이번 달을 4월로 두어 달 목록이 2월부터 4월까지다
    @Test
    @DisplayName("빈달있음_월별집계_0건달포함")
    void 빈달있음_월별집계_0건달포함() {
        given모은공고(
                source("S1", "Solicitation", LocalDateTime.of(2026, 2, 10, 9, 0), null),
                source("S2", "Solicitation", LocalDateTime.of(2026, 4, 3, 9, 0), null));
        Clock april = Clock.fixed(Instant.parse("2026-04-15T01:00:00Z"), KST);

        MonthlyResponse res = service(april).monthly();

        assertThat(res).extracting(r -> r.months().stream().map(m -> m.month() + " " + m.total()).toList(),
                        MonthlyResponse::emptyMonths)
                .containsExactly(List.of("2026-02 1", "2026-03 0", "2026-04 1"), List.of("2026-03"));
    }

    // TC-MKT-014
    @Test
    @DisplayName("유형묶음_월별집계_네묶음으로셈")
    void 유형묶음_월별집계_네묶음으로셈() {
        LocalDateTime posted = NOW_KST.minusDays(1);
        given모은공고(
                source("S1", "Combined Synopsis/Solicitation", posted, null),
                source("S2", "Presolicitation", posted, null),
                source("S3", "Award Notice", posted, null),
                source("S4", "Special Notice", posted, null));

        List<MonthStat> months = service().monthly().months();

        assertThat(months).singleElement()
                .extracting(MonthStat::bid, MonthStat::pre, MonthStat::award, MonthStat::other)
                .containsExactly(1L, 1L, 1L, 1L);
    }

    // TC-MKT-015
    @Test
    @DisplayName("31과33_업종집계_제조업한묶음")
    void 업종31과33_업종집계_제조업한묶음() {
        given모은공고(
                source("S1", "Solicitation", NOW_KST.minusDays(2), null, null, "311000"),
                source("S2", "Solicitation", NOW_KST.minusDays(1), null, null, "336110"));

        List<SectorStat> sectors = service().naics().sectors();

        assertThat(sectors).extracting(SectorStat::code, SectorStat::nameKo, SectorStat::count)
                .containsExactly(tuple("31-33", "제조업", 2L));
    }

    // TC-MKT-016
    @Test
    @DisplayName("업종코드없음_업종집계_표시없음으로셈")
    void 업종코드없음_업종집계_표시없음으로셈() {
        given모은공고(source("S1", "Solicitation", NOW_KST.minusDays(1), null, null, null));

        NaicsResponse res = service().naics();

        assertThat(List.of(res.noneCount(), res.codedCount())).containsExactly(1L, 0L);
    }

    // TC-MKT-017
    @Test
    @DisplayName("공공행정코드_업종이름_공공행정")
    void 공공행정코드_업종이름_공공행정() {
        given모은공고(source("S1", "Solicitation", NOW_KST.minusDays(1), null, null, "921190"));

        List<SectorStat> sectors = service().naics().sectors();

        assertThat(sectors).extracting(SectorStat::code, SectorStat::nameKo)
                .containsExactly(tuple("92", "공공 행정"));
    }
}
