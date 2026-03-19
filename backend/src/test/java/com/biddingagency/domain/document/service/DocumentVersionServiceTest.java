package com.biddingagency.domain.document.service;

import com.biddingagency.domain.bid.entity.BidRequest;
import com.biddingagency.domain.bid.repository.BidRequestRepository;
import com.biddingagency.domain.document.entity.BidDocument;
import com.biddingagency.domain.document.entity.BidDocumentVersion;
import com.biddingagency.domain.document.entity.DocumentStatus;
import com.biddingagency.domain.document.entity.DocumentType;
import com.biddingagency.domain.document.repository.BidDocumentRepository;
import com.biddingagency.domain.document.repository.BidDocumentVersionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * TC-EDIT-001 ~ TC-EDIT-007, TC-MCP-DOC-001 ~ TC-MCP-DOC-002: DocumentVersionService 테스트
 */
@ExtendWith(MockitoExtension.class)
class DocumentVersionServiceTest {

    @Mock
    private BidDocumentRepository documentRepository;
    @Mock
    private BidDocumentVersionRepository versionRepository;
    @Mock
    private BidRequestRepository bidRequestRepository;

    @InjectMocks
    private DocumentVersionService documentVersionService;

    private Map<String, Object> sampleContent() {
        return Map.of("type", "doc", "content", "test content");
    }

    // TC-EDIT-001: DRAFT 문서에 새 버전 저장
    @Test
    @DisplayName("DRAFT문서_새버전저장_currentVersionNo증가")
    void DRAFT_saveVersion_버전증가() {
        // given
        UUID docId = UUID.randomUUID();
        UUID editedBy = UUID.randomUUID();
        BidDocument document = BidDocument.builder()
                .documentType(DocumentType.COVER_LETTER)
                .status(DocumentStatus.DRAFT)
                .currentVersionNo(1)
                .build();
        given(documentRepository.findById(docId)).willReturn(Optional.of(document));
        given(versionRepository.save(any(BidDocumentVersion.class))).willAnswer(inv -> inv.getArgument(0));
        given(documentRepository.save(any(BidDocument.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        BidDocumentVersion result = documentVersionService.saveVersion(docId, sampleContent(), "수정", editedBy);

        // then
        assertThat(result.getVersionNo()).isEqualTo(2);
        assertThat(document.getCurrentVersionNo()).isEqualTo(2);
    }

    // TC-EDIT-002: LOCKED 문서 편집 시도 → 예외 (BIZ-003)
    @Test
    @DisplayName("LOCKED문서_편집시도_IllegalStateException_BIZ003")
    void LOCKED_saveVersion_예외() {
        // given
        UUID docId = UUID.randomUUID();
        BidDocument document = BidDocument.builder()
                .documentType(DocumentType.COVER_LETTER)
                .status(DocumentStatus.LOCKED)
                .currentVersionNo(1)
                .build();
        given(documentRepository.findById(docId)).willReturn(Optional.of(document));

        // when & then
        assertThatThrownBy(() -> documentVersionService.saveVersion(docId, sampleContent(), "수정", UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("LOCKED");
    }

    // TC-EDIT-003: 문서 잠금 (DRAFT → LOCKED)
    @Test
    @DisplayName("DRAFT문서_잠금_LOCKED상태변경")
    void DRAFT_lockDocument_LOCKED() {
        // given
        UUID docId = UUID.randomUUID();
        BidDocument document = BidDocument.builder()
                .documentType(DocumentType.COVER_LETTER)
                .status(DocumentStatus.DRAFT)
                .currentVersionNo(1)
                .build();
        given(documentRepository.findById(docId)).willReturn(Optional.of(document));
        given(documentRepository.save(any(BidDocument.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        documentVersionService.lockDocument(docId);

        // then
        assertThat(document.getStatus()).isEqualTo(DocumentStatus.LOCKED);
        assertThat(document.isLocked()).isTrue();
    }

    // TC-EDIT-004: 이미 LOCKED 문서 잠금 → 예외 (BIZ-009)
    @Test
    @DisplayName("이미LOCKED문서_잠금시도_IllegalStateException_BIZ009")
    void 이미LOCKED_lockDocument_예외() {
        // given
        UUID docId = UUID.randomUUID();
        BidDocument document = BidDocument.builder()
                .documentType(DocumentType.COVER_LETTER)
                .status(DocumentStatus.LOCKED)
                .currentVersionNo(1)
                .build();
        given(documentRepository.findById(docId)).willReturn(Optional.of(document));

        // when & then
        assertThatThrownBy(() -> documentVersionService.lockDocument(docId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already locked");
    }

    // TC-EDIT-007: 버전 불변성 검증 (새 버전만 생성 가능)
    @Test
    @DisplayName("새버전생성_기존버전불변_versionNo순차증가_BIZ002")
    void 버전불변성_saveVersion_순차증가() {
        // given
        UUID docId = UUID.randomUUID();
        UUID editedBy = UUID.randomUUID();
        BidDocument document = BidDocument.builder()
                .documentType(DocumentType.TECHNICAL_PROPOSAL)
                .status(DocumentStatus.DRAFT)
                .currentVersionNo(3)
                .build();
        given(documentRepository.findById(docId)).willReturn(Optional.of(document));
        given(versionRepository.save(any(BidDocumentVersion.class))).willAnswer(inv -> inv.getArgument(0));
        given(documentRepository.save(any(BidDocument.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        BidDocumentVersion result = documentVersionService.saveVersion(docId, sampleContent(), "v4", editedBy);

        // then
        assertThat(result.getVersionNo()).isEqualTo(4);
        assertThat(document.getCurrentVersionNo()).isEqualTo(4);
    }

    // TC-MCP-DOC-001: 신규 문서 생성 (createDocument)
    @Test
    @DisplayName("신규문서_createDocument_DRAFT상태_첫버전생성")
    void 신규_createDocument_DRAFT_첫버전() {
        // given
        UUID bidRequestId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        BidRequest bidRequest = BidRequest.builder().build();
        given(bidRequestRepository.findById(bidRequestId)).willReturn(Optional.of(bidRequest));
        given(documentRepository.existsByBidRequestIdAndDocumentType(bidRequestId, DocumentType.COVER_LETTER)).willReturn(false);
        given(documentRepository.save(any(BidDocument.class))).willAnswer(inv -> inv.getArgument(0));
        given(versionRepository.save(any(BidDocumentVersion.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        BidDocument result = documentVersionService.createDocument(bidRequestId, DocumentType.COVER_LETTER, sampleContent(), createdBy);

        // then
        assertThat(result.getStatus()).isEqualTo(DocumentStatus.DRAFT);
        assertThat(result.getCurrentVersionNo()).isEqualTo(1);
        assertThat(result.getDocumentType()).isEqualTo(DocumentType.COVER_LETTER);
    }

    // 중복 문서 타입 생성 시도
    @Test
    @DisplayName("이미존재하는문서타입_createDocument_예외")
    void 중복타입_createDocument_예외() {
        // given
        UUID bidRequestId = UUID.randomUUID();
        given(documentRepository.existsByBidRequestIdAndDocumentType(bidRequestId, DocumentType.COVER_LETTER)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> documentVersionService.createDocument(bidRequestId, DocumentType.COVER_LETTER, sampleContent(), UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already exists");
    }

    // LOCKED 문서 rollback 불가
    @Test
    @DisplayName("LOCKED문서_rollback시도_예외")
    void LOCKED_rollback_예외() {
        // given
        UUID docId = UUID.randomUUID();
        BidDocument document = BidDocument.builder()
                .documentType(DocumentType.COVER_LETTER)
                .status(DocumentStatus.LOCKED)
                .currentVersionNo(3)
                .build();
        given(documentRepository.findById(docId)).willReturn(Optional.of(document));

        // when & then
        assertThatThrownBy(() -> documentVersionService.rollbackToVersion(docId, 1, UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("LOCKED");
    }
}
