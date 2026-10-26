package com.biddingagency.domain.analysis.service;

import com.biddingagency.TestHelper;
import com.biddingagency.domain.analysis.entity.BidAnalysis;
import com.biddingagency.domain.analysis.service.BidAnalysisInputService.Step;
import com.biddingagency.domain.bid.entity.BidRequest;
import com.biddingagency.domain.bid.entity.BidRequestState;
import com.biddingagency.domain.bid.repository.BidRequestRepository;
import com.biddingagency.domain.compliance.service.ComplianceService;
import com.biddingagency.domain.eligibility.repository.EligibilityAnswerRepository;
import com.biddingagency.domain.member.entity.Member;
import com.biddingagency.domain.notice.repository.NoticeRepository;
import com.biddingagency.domain.opportunity.dto.MarketSource;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.repository.OpportunityRepository;
import com.biddingagency.domain.reading.service.DocumentReadingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.biddingagency.domain.analysis.service.BidAnalysisFixtures.capturePlan;
import static com.biddingagency.domain.analysis.service.BidAnalysisFixtures.m;
import static com.biddingagency.domain.analysis.service.BidAnalysisFixtures.report;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;

/** TC-DOC-268, 269, 292: get_bid_analysis_input (CR-170, CR-179, BID-DOC-006) */
@ExtendWith(MockitoExtension.class)
class BidAnalysisInputServiceTest {

    private static final String AGENCY = "411th CSB";

    @Mock private BidAnalysisService analysisService;
    @Mock private BidRequestRepository bidRequestRepository;
    @Mock private NoticeRepository noticeRepository;
    @Mock private OpportunityRepository opportunityRepository;
    @Mock private ComplianceService complianceService;
    @Mock private DocumentReadingService documentReadingService;
    @Mock private EligibilityAnswerRepository eligibilityAnswerRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private BidAnalysisInputService service;
    private BidRequest bid;
    private Opportunity opp;
    private BidAnalysis analysis;

    @BeforeEach
    void setUp() throws Exception {
        service = new BidAnalysisInputService(analysisService, bidRequestRepository, noticeRepository,
                opportunityRepository, complianceService, documentReadingService, eligibilityAnswerRepository,
                objectMapper);
        opp = TestHelper.withId(Opportunity.builder().title("Facility Support").solicitationNumber("W91QVN-25-R-0001")
                .organizationName(AGENCY).type("Solicitation").rawJson(Map.of("naicsCode", "561210")).build());
        Member member = TestHelper.withId(Member.builder().companyName("우진").build());
        bid = TestHelper.withId(BidRequest.builder().member(member).opportunity(opp)
                .state(BidRequestState.ANALYZING).stateHistory(new ArrayList<>()).build());
        analysis = TestHelper.withId(BidAnalysis.running(bid.getId(), null, LocalDateTime.now()));
        analysis.saveReport(objectMapper.writeValueAsString(report()));
        analysis.saveCapturePlan(objectMapper.writeValueAsString(capturePlan()));

        given(analysisService.analysisOf(bid.getId(), analysis.getId())).willReturn(analysis);
        given(bidRequestRepository.findByIdWithDetails(bid.getId())).willReturn(Optional.of(bid));
        given(noticeRepository.findFirstByOpportunityIdAndVisibilityOrderByAnalyzedAtDesc(any(), any()))
                .willReturn(Optional.empty());
        lenient().when(complianceService.designInputLines(any())).thenReturn(List.of());
    }

    private static MarketSource agencyNotice(int i) {
        return new MarketSource(UUID.randomUUID(), "N" + i, "SOL-" + i, "Notice " + i, null, "Solicitation", null,
                LocalDateTime.of(2025, 1, 1, 0, 0).plusDays(i), null, null, null, null, null, "561210", AGENCY, "Korea");
    }

    @Test
    @DisplayName("TC-DOC-268 입력REPORT_자료모음")
    @SuppressWarnings("unchecked")
    void REPORT_자료모음() {
        Opportunity amendment = TestHelper.withId(Opportunity.builder().type("Solicitation").title("Amendment 1").build());
        given(opportunityRepository.findSameSolicitation("W91QVN-25-R-0001", opp.getId())).willReturn(List.of(amendment));
        given(opportunityRepository.findRecentMarketSourcesByOrganization(eq(AGENCY), eq(opp.getId()), any(Pageable.class)))
                .willAnswer(inv -> {
                    List<MarketSource> all = new ArrayList<>();
                    for (int i = 0; i < 25; i++) all.add(agencyNotice(i));
                    return all.subList(0, Math.min(all.size(), inv.<Pageable>getArgument(2).getPageSize()));
                });
        List<Map<String, Object>> facts = List.of(m("readingId", "r1", "facts", List.of(m("value", "x".repeat(300)))));
        given(documentReadingService.designInputDocuments(bid.getId())).willReturn(facts);

        Map<String, Object> input = service.input(bid.getId(), analysis.getId(), Step.REPORT);

        assertThat((List<?>) input.get("sameSolicitationNotices")).hasSize(1);
        assertThat((List<?>) input.get("agencyRecentNotices")).hasSize(20);
        assertThat(input.get("clientDocuments")).isEqualTo(facts);
    }

    @Test
    @DisplayName("TC-DOC-269 입력DECISION_앞산출물과가중치")
    @SuppressWarnings("unchecked")
    void DECISION_앞산출물과가중치() {
        given(analysisService.policy()).willReturn(PwinPolicy.defaults());

        Map<String, Object> input = service.input(bid.getId(), analysis.getId(), Step.DECISION);

        assertThat((Map<String, Object>) input.get("report")).containsOnlyKeys("eligibility", "competitors", "fairness", "orderHistory");
        assertThat((Map<String, Object>) input.get("capturePlan")).containsKey("issues");
        assertThat((Map<String, Object>) ((Map<String, Object>) input.get("pwinPolicy")).get("weights"))
                .containsEntry("SOLUTION_FIT", 20);
    }

    @Test
    @DisplayName("TC-DOC-302 입력뒤단계_앞단계확인필요만")
    void 뒤단계_앞단계확인필요만() throws Exception {
        given(analysisService.policy()).willReturn(PwinPolicy.defaults());
        analysis.saveQuestions(objectMapper.writeValueAsString(List.of(
                m("target", "CUSTOMER", "question", "면허가 있습니까?", "reason", "자격", "part", "REPORT"),
                m("target", "ADMIN", "question", "마감이 연장되었습니까?", "reason", "일정", "part", "CAPTURE_PLAN"),
                m("target", "CUSTOMER", "question", "원가 자료를 주세요", "reason", "수익성", "part", "DECISION"))));

        Map<String, Object> input = service.input(bid.getId(), analysis.getId(), Step.DECISION);

        assertThat(input.get("priorQuestions")).isEqualTo(List.of(
                m("target", "CUSTOMER", "question", "면허가 있습니까?"),
                m("target", "ADMIN", "question", "마감이 연장되었습니까?")));
    }
}
