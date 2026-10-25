package com.biddingagency.domain.bid.service;

import com.biddingagency.TestHelper;
import com.biddingagency.domain.bid.entity.BidRequest;
import com.biddingagency.domain.bid.entity.BidRequestState;
import com.biddingagency.domain.document.entity.BidDocument;
import com.biddingagency.domain.document.entity.DocumentType;
import com.biddingagency.domain.document.service.BidDocumentService;
import com.biddingagency.domain.notification.entity.NotificationType;
import com.biddingagency.domain.notification.service.NotificationService;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.proposal.service.DocumentComposition;
import com.biddingagency.domain.proposal.service.DocumentCompositionService;
import com.biddingagency.integration.llmplatform.LLMPlatformClient;
import com.biddingagency.integration.llmplatform.LLMPlatformException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * 문서 작성 워크플로우가 실패하면 관리자 알림을 남긴다(IT-14-030).
 * 알림을 어느 길로 보내든 마지막은 NotificationService.sendNotification 이 notification_logs 줄을 남긴다
 */
@ExtendWith(MockitoExtension.class)
class AIWorkflowFailureNoticeTest {

    @Mock private LLMPlatformClient llmPlatformClient;
    @Mock private BidDocumentService bidDocumentService;
    @Mock private NotificationService notificationService;
    @Mock private DocumentCompositionService documentCompositionService;

    @InjectMocks
    private AIWorkflowService aiWorkflowService;

    @Test
    @Disabled("BL-002 문서 작성 실패 관리자 알림 미구현. 고치는 CR 이 걷는다")
    @DisplayName("IT-14-030 1 설계워크플로우실패_관리자에게AI_WORKFLOW_FAILED알림")
    void 설계워크플로우실패_관리자알림() {
        Opportunity opp = TestHelper.withId(Opportunity.builder().noticeId("N-1").title("Ground Maintenance").build());
        BidRequest br = TestHelper.withId(BidRequest.builder().opportunity(opp).build());
        br.transitionTo(BidRequestState.GENERATING, UUID.randomUUID(), "admin@example.com", null);
        given(documentCompositionService.compose(opp.getId()))
                .willReturn(DocumentComposition.of(null, List.of(DocumentType.TECHNICAL_PROPOSAL)));
        given(bidDocumentService.findByBidRequestAndType(br.getId(), DocumentType.TECHNICAL_PROPOSAL))
                .willReturn(TestHelper.withId(BidDocument.builder().build()));
        given(llmPlatformClient.runProposalDesign(any())).willThrow(new LLMPlatformException("워크플로우 실행 실패: runId=r-1"));

        aiWorkflowService.generateDocumentsAsync(br);

        then(notificationService).should().sendNotification(eq(NotificationType.AI_WORKFLOW_FAILED),
                any(), any(), any(), any(), eq(br.getId()), any(), any());
    }
}
