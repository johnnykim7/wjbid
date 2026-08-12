package com.biddingagency.mcp.tool;

import com.biddingagency.domain.bid.entity.BidRequest;
import com.biddingagency.domain.bid.entity.BidRequestState;
import com.biddingagency.domain.bid.entity.StateTransition;
import com.biddingagency.domain.bid.service.BidRequestService;
import com.biddingagency.domain.document.entity.BidDocument;
import com.biddingagency.domain.document.entity.BidDocumentVersion;
import com.biddingagency.domain.document.entity.DocumentStatus;
import com.biddingagency.domain.document.entity.DocumentType;
import com.biddingagency.domain.document.service.BidDocumentService;
import com.biddingagency.domain.document.service.DocumentVersionService;
import com.biddingagency.domain.event.DocumentGeneratedEvent;
import com.biddingagency.domain.member.entity.Member;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.TestHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * TC-MCP-DOC-001 ~ TC-MCP-DOC-004: DocumentMcpTool 테스트
 */
@ExtendWith(MockitoExtension.class)
class DocumentMcpToolTest {

    @Mock
    private BidRequestService bidRequestService;
    @Mock
    private BidDocumentService bidDocumentService;
    @Mock
    private DocumentVersionService documentVersionService;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private DocumentMcpTool documentMcpTool;

    // TC-MCP-DOC-001: save_document_version 신규 문서 생성
    @Test
    @DisplayName("신규문서_saveDocumentVersion_BidDocument생성_첫버전")
    void 신규_saveDocumentVersion_문서생성() {
        // given
        UUID bidRequestId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        Map<String, Object> contentJson = Map.of("type", "doc", "content", List.of());
        BidDocument newDoc = TestHelper.withId(BidDocument.builder()
                .documentType(DocumentType.COVER_LETTER)
                .status(DocumentStatus.DRAFT)
                .currentVersionNo(1)
                .build(), docId);
        BidDocumentVersion version = BidDocumentVersion.builder()
                .document(newDoc)
                .versionNo(1)
                .contentJson(contentJson)
                .editedBy(new UUID(0L, 0L))
                .editedAt(LocalDateTime.now())
                .changeSummary("LLM 생성 문서")
                .build();

        UUID adminId = UUID.randomUUID();
        givenBidRequestWithGeneratingHistory(bidRequestId, adminId);

        // 기존 문서 없음(Optional.empty) → 신규 생성 분기
        given(bidDocumentService.findOptionalByBidRequestAndType(eq(bidRequestId), eq(DocumentType.COVER_LETTER)))
                .willReturn(Optional.empty());
        given(documentVersionService.createDocument(eq(bidRequestId), eq(DocumentType.COVER_LETTER), any(), any()))
                .willReturn(newDoc);
        given(documentVersionService.getLatestVersion(any())).willReturn(version);

        Map<String, Object> args = new LinkedHashMap<>();
        args.put("bidRequestId", bidRequestId.toString());
        args.put("documentType", "COVER_LETTER");
        args.put("contentJson", contentJson);

        // when
        String result = documentMcpTool.saveDocumentVersion(args);

        // then
        assertThat(result).contains("COVER_LETTER");
        assertThat(result).contains("v1");

        // CR-017: 문서 생성 완료 이벤트 발행 검증
        ArgumentCaptor<DocumentGeneratedEvent> captor = ArgumentCaptor.forClass(DocumentGeneratedEvent.class);
        then(eventPublisher).should().publishEvent(captor.capture());
        DocumentGeneratedEvent event = captor.getValue();
        assertThat(event.getBidRequestId()).isEqualTo(bidRequestId);
        assertThat(event.getDocumentType()).isEqualTo("COVER_LETTER");
        assertThat(event.getVersionNo()).isEqualTo(1);
    }

    /**
     * saveDocumentVersion은 edited_by를 state_history의 마지막 GENERATING 전이 트리거 관리자로 정한다
     * (2026-05-31 합의: AI는 도구일 뿐 책임자는 관리자). 따라서 GENERATING 이력을 가진 BidRequest가 필요하다.
     */
    private BidRequest givenBidRequestWithGeneratingHistory(UUID bidRequestId, UUID adminId) {
        List<StateTransition> history = new ArrayList<>();
        history.add(StateTransition.builder()
                .toState(BidRequestState.GENERATING)
                .userId(adminId)
                .build());
        BidRequest bidRequest = TestHelper.withId(BidRequest.builder()
                .state(BidRequestState.GENERATING)
                .stateHistory(history)
                .build(), bidRequestId);
        given(bidRequestService.findByIdWithDetails(bidRequestId)).willReturn(bidRequest);
        return bidRequest;
    }

    // TC-MCP-DOC-002: save_document_version 추가 버전
    @Test
    @DisplayName("기존문서_saveDocumentVersion_새버전추가_기존불변_BIZ002")
    void 기존문서_saveDocumentVersion_버전추가() {
        // given
        UUID bidRequestId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        Map<String, Object> contentJson = Map.of("type", "doc", "content", List.of());
        BidDocument existingDoc = TestHelper.withId(BidDocument.builder()
                .documentType(DocumentType.TECHNICAL_PROPOSAL)
                .status(DocumentStatus.DRAFT)
                .currentVersionNo(1)
                .build(), docId);
        BidDocumentVersion newVersion = BidDocumentVersion.builder()
                .document(existingDoc)
                .versionNo(2)
                .contentJson(contentJson)
                .editedBy(new UUID(0L, 0L))
                .editedAt(LocalDateTime.now())
                .changeSummary("LLM 생성 문서")
                .build();

        givenBidRequestWithGeneratingHistory(bidRequestId, UUID.randomUUID());

        given(bidDocumentService.findOptionalByBidRequestAndType(eq(bidRequestId), eq(DocumentType.TECHNICAL_PROPOSAL)))
                .willReturn(Optional.of(existingDoc));
        given(documentVersionService.saveVersion(any(), any(), anyString(), any()))
                .willReturn(newVersion);

        Map<String, Object> args = new LinkedHashMap<>();
        args.put("bidRequestId", bidRequestId.toString());
        args.put("documentType", "TECHNICAL_PROPOSAL");
        args.put("contentJson", contentJson);

        // when
        String result = documentMcpTool.saveDocumentVersion(args);

        // then
        assertThat(result).contains("TECHNICAL_PROPOSAL");
        assertThat(result).contains("v2");

        // CR-017: 새 버전 저장 시에도 이벤트 발행 (멱등키에 버전 포함되어 재생성 알림 가능)
        ArgumentCaptor<DocumentGeneratedEvent> captor = ArgumentCaptor.forClass(DocumentGeneratedEvent.class);
        then(eventPublisher).should().publishEvent(captor.capture());
        DocumentGeneratedEvent event = captor.getValue();
        assertThat(event.getDocumentType()).isEqualTo("TECHNICAL_PROPOSAL");
        assertThat(event.getVersionNo()).isEqualTo(2);
    }

    // TC-MCP-DOC-003: get_bid_request
    @Test
    @DisplayName("유효bidRequestId_getBidRequest_입찰정보_문서목록JSON반환")
    void 유효ID_getBidRequest_정보반환() {
        // given
        UUID bidRequestId = UUID.randomUUID();
        Member member = TestHelper.withId(Member.builder().email("t@t.com").passwordHash("h").companyName("Co").build());
        Opportunity opp = TestHelper.withId(Opportunity.builder()
                .noticeId("N-1").title("T").active(true)
                .firstSeenAt(LocalDateTime.now()).lastModifiedAt(LocalDateTime.now()).build());
        BidRequest bidRequest = TestHelper.withId(BidRequest.builder()
                .member(member)
                .opportunity(opp)
                .state(BidRequestState.REVIEW)
                .stateHistory(new ArrayList<>())
                .build(), bidRequestId);

        given(bidRequestService.findByIdWithDetails(bidRequestId)).willReturn(bidRequest);
        given(bidDocumentService.findByBidRequest(bidRequestId)).willReturn(List.of());

        // when
        String result = documentMcpTool.getBidRequest(Map.of("bidRequestId", bidRequestId.toString()));

        // then
        assertThat(result).contains("REVIEW");
        assertThat(result).contains("documents");
    }

    // 잘못된 documentType
    @Test
    @DisplayName("잘못된documentType_saveDocumentVersion_예외")
    void 잘못된타입_saveDocumentVersion_예외() {
        // given: documentType 파싱은 bidRequest 조회 뒤에 오므로, 그 앞 단계를 통과시켜야 본 검증에 도달한다
        UUID bidRequestId = UUID.randomUUID();
        givenBidRequestWithGeneratingHistory(bidRequestId, UUID.randomUUID());

        Map<String, Object> args = new LinkedHashMap<>();
        args.put("bidRequestId", bidRequestId.toString());
        args.put("documentType", "INVALID_TYPE");
        args.put("contentJson", Map.of());

        // when & then
        assertThatThrownBy(() -> documentMcpTool.saveDocumentVersion(args))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("알 수 없는 documentType");
    }
}
