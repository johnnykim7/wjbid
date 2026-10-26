package com.biddingagency.mcp.tool;

import com.biddingagency.TestHelper;
import com.biddingagency.domain.bid.repository.BidRequestRepository;
import com.biddingagency.domain.document.repository.BidDocumentRepository;
import com.biddingagency.domain.notice.entity.Notice;
import com.biddingagency.domain.notice.entity.NoticeGenerationStatus;
import com.biddingagency.domain.notice.service.NoticeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * TC-OPP-021: 공고 분석이 남긴 평가 항목별 문서 종류와 커버레터 요구 여부를 그대로 저장한다 (CR-180, BIZ-096)
 */
@ExtendWith(MockitoExtension.class)
class OpportunityAnalysisMcpToolTest {

    @Mock private NoticeService noticeService;
    @Mock private BidRequestRepository bidRequestRepository;
    @Mock private BidDocumentRepository bidDocumentRepository;

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("TC-OPP-021 공고분석저장_문서종류와커버레터요구_그대로담김")
    void 공고분석저장_문서종류와커버레터요구_그대로담김() {
        OpportunityAnalysisMcpTool tool = new OpportunityAnalysisMcpTool(noticeService, bidRequestRepository,
                bidDocumentRepository, new ObjectMapper());
        UUID noticeId = UUID.randomUUID();
        Map<String, Object> factorTree = Map.of("method", "LPTA", "coverLetterRequired", false, "factors", List.of(
                Map.of("factorId", "F1", "documentType", "TECHNICAL_PROPOSAL"),
                Map.of("factorId", "F2", "documentType", "PAST_PERFORMANCE")));
        given(noticeService.saveResult(eq(noticeId), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .willReturn(TestHelper.withId(Notice.builder().generationStatus(NoticeGenerationStatus.COMPLETED).build()));

        tool.saveOpportunityAnalysis(Map.of("noticeId", noticeId.toString(), "koreanTitle", "제목", "factorTree", factorTree));

        ArgumentCaptor<Map<String, Object>> saved = ArgumentCaptor.forClass(Map.class);
        then(noticeService).should().saveResult(eq(noticeId), any(), any(), any(), any(), any(), any(), any(), any(),
                saved.capture());
        assertThat(saved.getValue()).isEqualTo(factorTree);
    }
}
