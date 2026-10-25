package com.biddingagency.domain.proposal.service;

import com.biddingagency.TestHelper;
import com.biddingagency.domain.bid.entity.BidRequest;
import com.biddingagency.domain.document.entity.BidDocument;
import com.biddingagency.domain.document.entity.BidDocumentVersion;
import com.biddingagency.domain.document.entity.DocumentType;
import com.biddingagency.domain.document.service.DocumentVersionService;
import com.biddingagency.domain.event.DocumentGeneratedEvent;
import com.biddingagency.domain.proposal.entity.ProposalBlock;
import com.biddingagency.domain.proposal.entity.ProposalChapter;
import com.biddingagency.domain.proposal.entity.ProposalSection;
import com.biddingagency.domain.proposal.service.ProposalService.ChapterWithSections;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * 제안서 묶기 (CR-169, TC-DOC-239~242).
 */
@ExtendWith(MockitoExtension.class)
class ProposalAssembleServiceTest {

    @Mock private ProposalService proposalService;
    @Mock private DocumentVersionService documentVersionService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private ProposalAssembleService assembleService;

    private final UUID documentId = UUID.randomUUID();

    private static ProposalChapter chapter(String label, String title) {
        return TestHelper.withId(ProposalChapter.builder().factorLabel(label).factorTitle(title).build());
    }

    private static ProposalSection section(String label, String title) {
        return TestHelper.withId(ProposalSection.builder().subfactorLabel(label).title(title).build());
    }

    private static ProposalBlock block(Map<String, Object> node) {
        return ProposalBlock.builder().contentJson(node).build();
    }

    private static Map<String, Object> paragraph(String text) {
        return Map.of("type", "paragraph", "content", List.of(Map.of("type", "text", "text", text)));
    }

    private static Map<String, Object> heading(int level, String text) {
        return Map.of("type", "heading", "attrs", Map.of("level", level),
                "content", List.of(Map.of("type", "text", "text", text)));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> content(Map<String, Object> doc) {
        return (List<Map<String, Object>>) doc.get("content");
    }

    @Test
    @DisplayName("TC-DOC-239 장절블록_순서대로_장제목1단_절제목2단_라벨없으면제목만")
    void 장절블록_묶기_순서와제목() {
        ProposalChapter c1 = chapter("I", "Technical Approach");
        ProposalChapter c2 = chapter(null, "Past Performance");
        ProposalSection s11 = section("A", "Staffing");
        ProposalSection s12 = section(null, "Schedule");
        ProposalSection s21 = section("A", "Contracts");
        ProposalSection s22 = section(" ", "References");
        given(proposalService.getTree(documentId)).willReturn(List.of(
                new ChapterWithSections(c1, List.of(s11, s12)),
                new ChapterWithSections(c2, List.of(s21, s22))));
        given(proposalService.getBlocks(s11.getId())).willReturn(List.of(block(paragraph("p1")), block(paragraph("p2"))));
        given(proposalService.getBlocks(s12.getId())).willReturn(List.of(block(paragraph("p3")), block(paragraph("p4"))));
        given(proposalService.getBlocks(s21.getId())).willReturn(List.of(block(paragraph("p5")), block(paragraph("p6"))));
        given(proposalService.getBlocks(s22.getId())).willReturn(List.of(block(paragraph("p7")), block(paragraph("p8"))));

        Map<String, Object> doc = assembleService.buildDocument(documentId);

        assertThat(content(doc)).containsExactly(
                heading(1, "I. Technical Approach"),
                heading(2, "A Staffing"), paragraph("p1"), paragraph("p2"),
                heading(2, "Schedule"), paragraph("p3"), paragraph("p4"),
                heading(1, "Past Performance"),
                heading(2, "A Contracts"), paragraph("p5"), paragraph("p6"),
                heading(2, "References"), paragraph("p7"), paragraph("p8"));
    }

    @Test
    @DisplayName("TC-DOC-240 블록없는절_제목만남고_다음절로이어짐")
    void 블록없는절_묶기_제목만() {
        ProposalSection empty = section("A", "Staffing");
        ProposalSection next = section("B", "Schedule");
        given(proposalService.getTree(documentId)).willReturn(List.of(
                new ChapterWithSections(chapter("I", "Technical"), List.of(empty, next))));
        given(proposalService.getBlocks(empty.getId())).willReturn(List.of());
        given(proposalService.getBlocks(next.getId())).willReturn(List.of(block(paragraph("p1"))));

        Map<String, Object> doc = assembleService.buildDocument(documentId);

        assertThat(content(doc)).containsExactly(
                heading(1, "I. Technical"), heading(2, "A Staffing"), heading(2, "B Schedule"), paragraph("p1"));
    }

    @Test
    @DisplayName("TC-DOC-241 doc로싼블록_안의내용만붙음")
    void doc로싼블록_묶기_안만붙임() {
        ProposalSection s = section("A", "Staffing");
        given(proposalService.getTree(documentId)).willReturn(List.of(
                new ChapterWithSections(chapter("I", "Technical"), List.of(s))));
        given(proposalService.getBlocks(s.getId())).willReturn(List.of(
                block(Map.of("type", "doc", "content", List.of(paragraph("p1"), paragraph("p2"))))));

        Map<String, Object> doc = assembleService.buildDocument(documentId);

        assertThat(content(doc)).extracting(n -> n.get("type")).doesNotContain("doc");
    }

    @Test
    @DisplayName("TC-DOC-242 빈버전1위에_새버전저장_편집자는작성시킨관리자_완료이벤트1회")
    void 묶기_새버전_편집자와알림() {
        UUID admin = UUID.randomUUID();
        BidRequest bidRequest = TestHelper.withId(BidRequest.builder().build());
        BidDocument document = TestHelper.withId(BidDocument.builder()
                .bidRequest(bidRequest).documentType(DocumentType.TECHNICAL_PROPOSAL).currentVersionNo(2).build(), documentId);
        BidDocumentVersion v2 = BidDocumentVersion.builder().document(document).versionNo(2).build();
        given(proposalService.getTree(documentId)).willReturn(List.of());
        given(documentVersionService.saveVersion(eq(documentId), anyMap(), any(), eq(admin))).willReturn(v2);

        assembleService.assemble(documentId, admin);

        ArgumentCaptor<DocumentGeneratedEvent> event = ArgumentCaptor.forClass(DocumentGeneratedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().getVersionNo()).isEqualTo(2);
    }
}
