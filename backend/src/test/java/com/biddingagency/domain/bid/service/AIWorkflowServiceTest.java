package com.biddingagency.domain.bid.service;

import com.biddingagency.domain.bid.entity.BidRequest;
import com.biddingagency.domain.bid.repository.ClientDocumentRepository;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.repository.OpportunityAnalysisRepository;
import com.biddingagency.domain.opportunity.repository.OpportunityRequirementItemRepository;
import com.biddingagency.domain.rfp.entity.IndustryType;
import com.biddingagency.domain.rfp.entity.PatternGuide;
import com.biddingagency.domain.rfp.repository.PatternGuideRepository;
import com.biddingagency.domain.rfp.service.ReferenceSampleService;
import com.biddingagency.integration.llmplatform.LLMPlatformClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;

/**
 * AIWorkflowService.build3PipelineContext 테스트 (CR-014).
 * P3 교체 검증 — industry_type=null이면 빈 처리, 비-null이면 successGuide+referenceSamples 채움.
 */
@ExtendWith(MockitoExtension.class)
class AIWorkflowServiceTest {

    @Mock private LLMPlatformClient llmPlatformClient;
    @Mock private OpportunityRequirementItemRepository requirementItemRepository;
    @Mock private OpportunityAnalysisRepository opportunityAnalysisRepository;
    @Mock private ClientDocumentRepository clientDocumentRepository;
    @Mock private PatternGuideRepository patternGuideRepository;
    @Mock private ReferenceSampleService referenceSampleService;

    @InjectMocks
    private AIWorkflowService aiWorkflowService;

    private BidRequest bidRequest(Opportunity opp) {
        UUID bidRequestId = UUID.randomUUID();
        BidRequest br = BidRequest.builder().opportunity(opp).build();
        ReflectionTestUtils.setField(br, "id", bidRequestId);
        return br;
    }

    private Opportunity opportunity(IndustryType type) {
        Opportunity opp = Opportunity.builder()
                .noticeId("N-1").title("Ground Maintenance at Kunsan AB").industryType(type)
                .build();
        ReflectionTestUtils.setField(opp, "id", UUID.randomUUID());
        return opp;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> invokeContext(BidRequest br, Opportunity opp) {
        // P1/P2 공통 stub (조건분기 없음 — 항상 조회)
        lenient().when(opportunityAnalysisRepository.findByOpportunityId(opp.getId()))
                .thenReturn(Optional.empty());
        lenient().when(clientDocumentRepository.findByBidRequestId(br.getId()))
                .thenReturn(List.of());
        return (Map<String, Object>) ReflectionTestUtils.invokeMethod(
                aiWorkflowService, "build3PipelineContext", br, opp);
    }

    @Test
    @DisplayName("미분류공고_P3빈처리_successGuide_null_referenceSamples_빈")
    void context_미분류_빈처리() {
        Opportunity opp = opportunity(null);
        BidRequest br = bidRequest(opp);
        given(referenceSampleService.collect(null)).willReturn(List.of());

        Map<String, Object> context = invokeContext(br, opp);

        assertThat(context).containsKey("successGuide");
        assertThat(context.get("successGuide")).isNull();
        assertThat((List<?>) context.get("referenceSamples")).isEmpty();
        assertThat(context.get("referenceUsagePolicy")).asString().contains("복사하지 마세요");
        // P3 교체 확인 — 구 pastSubmissions 키는 더 이상 없음
        assertThat(context).doesNotContainKey("pastSubmissions");
    }

    @Test
    @DisplayName("유형분류공고_가이드와원본메타_채움")
    void context_유형분류_채움() {
        Opportunity opp = opportunity(IndustryType.GROUND_MAINTENANCE);
        BidRequest br = bidRequest(opp);

        PatternGuide guide = PatternGuide.builder()
                .industryType(IndustryType.GROUND_MAINTENANCE).build();
        guide.markCompleted(Map.of("checklist", List.of("실적은 표로 12건+")), 2);

        given(patternGuideRepository.findByIndustryType(IndustryType.GROUND_MAINTENANCE))
                .willReturn(Optional.of(guide));
        given(referenceSampleService.collect(IndustryType.GROUND_MAINTENANCE))
                .willReturn(List.of(Map.of("rfpSampleId", "s1", "files", List.of())));

        Map<String, Object> context = invokeContext(br, opp);

        assertThat(context.get("successGuide")).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> sg = (Map<String, Object>) context.get("successGuide");
        assertThat(sg).containsKey("checklist");
        assertThat((List<?>) context.get("referenceSamples")).hasSize(1);
    }
}
