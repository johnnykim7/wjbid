package com.biddingagency.domain.proposal.service;

import com.biddingagency.domain.document.entity.BidDocument;
import com.biddingagency.domain.document.entity.BidDocumentVersion;
import com.biddingagency.domain.document.service.DocumentVersionService;
import com.biddingagency.domain.event.DocumentGeneratedEvent;
import com.biddingagency.domain.proposal.entity.ProposalBlock;
import com.biddingagency.domain.proposal.entity.ProposalChapter;
import com.biddingagency.domain.proposal.entity.ProposalSection;
import com.biddingagency.domain.proposal.service.ProposalService.ChapterWithSections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 제안서 묶기 (CR-028 설계, CR-169 구현).
 *
 * 장, 절, 블록 순서로 본문을 이어 TipTap 문서 하나를 새 버전으로 저장한다. 본문은 고치지 않는다.
 * AI 가 묶으면 문서 전체를 한 번에 읽고 다시 써야 해 도구 결과 한도에 걸리고 본문을 줄여 쓸 수 있어 서버가 묶는다.
 * 문서 전체 문체 손질은 CR-173 에서 다룬다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProposalAssembleService {

    static final String CHANGE_SUMMARY = "Assembled from proposal sections";

    private final ProposalService proposalService;
    private final DocumentVersionService documentVersionService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public BidDocumentVersion assemble(UUID documentId, UUID editorId) {
        BidDocumentVersion version = documentVersionService.saveVersion(
                documentId, buildDocument(documentId), CHANGE_SUMMARY, editorId);
        BidDocument document = version.getDocument();
        eventPublisher.publishEvent(new DocumentGeneratedEvent(
                documentId, document.getBidRequest().getId(), document.getDocumentType().name(), version.getVersionNo()));
        log.info("[AI] 제안서 묶기 완료: documentId={}, version={}", documentId, version.getVersionNo());
        return version;
    }

    /** 장 제목과 절 제목은 관리자 제안서 Section 관리 화면이 보이는 꼴과 같게 붙인다. */
    Map<String, Object> buildDocument(UUID documentId) {
        List<Map<String, Object>> content = new ArrayList<>();
        for (ChapterWithSections tree : proposalService.getTree(documentId)) {
            ProposalChapter chapter = tree.chapter();
            content.add(heading(1, joinLabel(chapter.getFactorLabel(), ". ", chapter.getFactorTitle())));
            for (ProposalSection section : tree.sections()) {
                content.add(heading(2, joinLabel(section.getSubfactorLabel(), " ", section.getTitle())));
                for (ProposalBlock block : proposalService.getBlocks(section.getId())) {
                    appendBlock(content, block.getContentJson());
                }
            }
        }
        return Map.of("type", "doc", "content", content);
    }

    /** 블록은 TipTap 노드 하나지만 AI 가 doc 로 싸서 보내도 doc 안에 doc 가 생기지 않게 안의 내용만 붙인다. */
    @SuppressWarnings("unchecked")
    private void appendBlock(List<Map<String, Object>> content, Map<String, Object> node) {
        if (node == null) return;
        if ("doc".equals(node.get("type"))) {
            if (node.get("content") instanceof List<?> children) {
                children.forEach(c -> content.add((Map<String, Object>) c));
            }
            return;
        }
        content.add(node);
    }

    private static String joinLabel(String label, String separator, String title) {
        boolean hasLabel = label != null && !label.isBlank();
        boolean hasTitle = title != null && !title.isBlank();
        if (hasLabel && hasTitle) return label + separator + title;
        return hasLabel ? label : (hasTitle ? title : "");
    }

    /** TipTap 은 빈 글자 노드를 받지 않아 제목이 비면 내용 없는 제목으로 둔다. */
    private static Map<String, Object> heading(int level, String text) {
        if (text.isEmpty()) {
            return Map.of("type", "heading", "attrs", Map.of("level", level));
        }
        return Map.of("type", "heading", "attrs", Map.of("level", level),
                "content", List.of(Map.of("type", "text", "text", text)));
    }
}
