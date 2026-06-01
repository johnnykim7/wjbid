package com.biddingagency.domain.proposal.service;

import com.biddingagency.domain.proposal.entity.*;
import com.biddingagency.domain.proposal.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Proposal 영속화 서비스 (CR-027).
 *
 * chapter/section/block 트리 CRUD + 부분 재생성용 block 교체 + section 상태 전이/잠금.
 * 파이프라인 호출(design/write-section)은 CR-028, 재생성 API/UI 는 CR-030 에서 이 서비스를 사용.
 *
 * 로직은 Service 에만 (CLAUDE.md). 트리 일관성·LOCKED 안전장치를 여기서 강제.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProposalService {

    private final ProposalChapterRepository chapterRepository;
    private final ProposalSectionRepository sectionRepository;
    private final ProposalBlockRepository blockRepository;

    // ─────────────────────────────────────────────
    // 트리 조회
    // ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ProposalChapter> getChapters(UUID documentId) {
        return chapterRepository.findByDocumentIdOrderByOrderNoAsc(documentId);
    }

    @Transactional(readOnly = true)
    public List<ProposalSection> getSections(UUID chapterId) {
        return sectionRepository.findByChapterIdOrderByOrderNoAsc(chapterId);
    }

    @Transactional(readOnly = true)
    public List<ProposalSection> getAllSections(UUID documentId) {
        return sectionRepository.findAllByDocumentId(documentId);
    }

    @Transactional(readOnly = true)
    public List<ProposalBlock> getBlocks(UUID sectionId) {
        return blockRepository.findBySectionIdOrderByOrderNoAsc(sectionId);
    }

    @Transactional(readOnly = true)
    public ProposalSection getSection(UUID sectionId) {
        return sectionRepository.findById(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("Section not found: " + sectionId));
    }

    // ─────────────────────────────────────────────
    // 트리 생성 (design 단계 — CR-028 이 호출)
    // ─────────────────────────────────────────────

    @Transactional
    public ProposalChapter createChapter(UUID documentId, String factorLabel, String factorTitle,
                                         SourceSection sourceSection, int orderNo) {
        ProposalChapter chapter = ProposalChapter.builder()
                .documentId(documentId)
                .factorLabel(factorLabel)
                .factorTitle(factorTitle)
                .sourceSection(sourceSection != null ? sourceSection : SourceSection.NOTICE_M)
                .orderNo(orderNo)
                .build();
        return chapterRepository.save(chapter);
    }

    @Transactional
    public ProposalSection createSection(UUID chapterId, String subfactorLabel, String title,
                                         String scope, List<String> requirementRefs,
                                         Integer minWords, int orderNo) {
        ProposalSection section = ProposalSection.builder()
                .chapterId(chapterId)
                .subfactorLabel(subfactorLabel)
                .title(title)
                .scope(scope)
                .requirementRefs(requirementRefs)
                .minWords(minWords)
                .orderNo(orderNo)
                .status(SectionStatus.PENDING)
                .build();
        return sectionRepository.save(section);
    }

    // ─────────────────────────────────────────────
    // block 교체 (write-section 단계 — CR-028 이 호출)
    // 부분 재생성: 해당 section 의 기존 block 전량 삭제 후 새 block 저장.
    // ─────────────────────────────────────────────

    @Transactional
    public List<ProposalBlock> replaceBlocks(UUID sectionId, List<BlockInput> blocks) {
        ProposalSection section = getSection(sectionId);
        if (section.isLocked()) {
            throw new IllegalStateException("Cannot replace blocks of LOCKED section: " + sectionId);
        }

        blockRepository.deleteBySectionId(sectionId);
        blockRepository.flush();

        int order = 0;
        for (BlockInput in : blocks) {
            ProposalBlock block = ProposalBlock.builder()
                    .sectionId(sectionId)
                    .blockType(in.blockType() != null ? in.blockType() : BlockType.PARAGRAPH)
                    .contentJson(in.contentJson())
                    .sourceEvidence(in.sourceEvidence())
                    .orderNo(order++)
                    .build();
            blockRepository.save(block);
        }
        section.markDrafted();
        return blockRepository.findBySectionIdOrderByOrderNoAsc(sectionId);
    }

    // ─────────────────────────────────────────────
    // section 상태 전이 / 잠금 (CR-030 이 사용)
    // ─────────────────────────────────────────────

    @Transactional
    public void markDrafting(UUID sectionId) {
        getSection(sectionId).markDrafting();
    }

    @Transactional
    public void markVerified(UUID sectionId) {
        getSection(sectionId).markVerified();
    }

    @Transactional
    public void markNeedsRegen(UUID sectionId) {
        getSection(sectionId).markNeedsRegen();
    }

    @Transactional
    public void lockSection(UUID sectionId) {
        getSection(sectionId).lock();
    }

    @Transactional
    public void unlockSection(UUID sectionId) {
        getSection(sectionId).unlock();
    }

    /** block 교체 입력 (DTO 의존 없이 Service 경계에서 받는 단순 record). */
    public record BlockInput(
            BlockType blockType,
            Map<String, Object> contentJson,
            Map<String, Object> sourceEvidence
    ) {}
}
