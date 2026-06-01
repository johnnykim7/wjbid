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
    private final VerificationLogService verificationLogService;

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

    /**
     * CR-030 트리 조립 — chapter + 하위 section 을 orderNo 순으로 묶어 반환.
     * 좌측 트리 렌더용. block 본문은 미포함 (section 선택 시 {@link #getBlocks}).
     */
    @Transactional(readOnly = true)
    public List<ChapterWithSections> getTree(UUID documentId) {
        List<ProposalChapter> chapters = chapterRepository.findByDocumentIdOrderByOrderNoAsc(documentId);
        return chapters.stream()
                .map(c -> new ChapterWithSections(c, getSections(c.getId())))
                .toList();
    }

    /** 트리 조립 결과 (Controller 가 DTO 로 매핑). */
    public record ChapterWithSections(ProposalChapter chapter, List<ProposalSection> sections) {}

    @Transactional(readOnly = true)
    public ProposalChapter getChapter(UUID chapterId) {
        return chapterRepository.findById(chapterId)
                .orElseThrow(() -> new IllegalArgumentException("Chapter not found: " + chapterId));
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
    // 트리 일괄 저장 (design 단계 콜백 — CR-028 save_proposal_structure 가 호출)
    // design WF 가 chapter/section 트리를 통째로 콜백 → 기존 트리 비우고 재생성.
    // LOCKED section 이 하나라도 있으면 사람이 다듬은 산출물 보호를 위해 전체 거부.
    // ─────────────────────────────────────────────

    @Transactional
    public List<ProposalChapter> saveStructure(UUID documentId, List<ChapterInput> chapters) {
        // LOCKED 보호: 기존 트리에 LOCKED section 이 있으면 통째 재생성 금지
        List<ProposalSection> existing = sectionRepository.findAllByDocumentId(documentId);
        boolean hasLocked = existing.stream().anyMatch(ProposalSection::isLocked);
        if (hasLocked) {
            throw new IllegalStateException(
                "LOCKED section 이 있는 문서는 design 재생성 불가 (documentId=" + documentId + "). 개별 section 만 재작성하세요.");
        }

        // 기존 chapter 삭제 → section/block 은 FK ON DELETE CASCADE 로 함께 제거
        chapterRepository.deleteByDocumentId(documentId);
        chapterRepository.flush();

        int chapterOrder = 0;
        for (ChapterInput ci : chapters) {
            ProposalChapter chapter = createChapter(documentId, ci.factorLabel(), ci.factorTitle(),
                    parseSourceSection(ci.sourceSection()), chapterOrder++);
            int sectionOrder = 0;
            List<SectionInput> sections = ci.sections() != null ? ci.sections() : List.of();
            for (SectionInput si : sections) {
                createSection(chapter.getId(), si.subfactorLabel(), si.title(), si.scope(),
                        si.requirementRefs(), si.minWords(), sectionOrder++);
            }
        }
        return chapterRepository.findByDocumentIdOrderByOrderNoAsc(documentId);
    }

    private SourceSection parseSourceSection(String raw) {
        if (raw == null || raw.isBlank()) return SourceSection.NOTICE_M;
        try {
            return SourceSection.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            return SourceSection.NOTICE_M;
        }
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

        // CR-031 BE 정형 룰 (LLM 0콜): block 0개 / min_words 미달 평가 → verification_log(RULE) 적재.
        // 위반 시 NEEDS_REGEN, 통과 시 DRAFTED. LLM 충실성 검증은 호출부(AIWorkflowService)가 후속으로 돌린다.
        int wordCount = countWords(blocks);
        int attempt = verificationLogService.nextAttempt(VerificationTargetType.PROPOSAL_SECTION, sectionId);
        List<String> findings = verificationLogService.evaluateSectionRules(
                blocks.size(), wordCount, section.getMinWords());
        verificationLogService.recordRule(
                VerificationTargetType.PROPOSAL_SECTION, sectionId, findings, wordCount, attempt);

        if (findings.isEmpty()) {
            section.markDrafted();
        } else {
            log.warn("[CR-031] section 정형 룰 위반 → NEEDS_REGEN: sectionId={}, findings={}", sectionId, findings);
            section.markDrafted();   // 본문은 존재하므로 일단 DRAFTED 로 둔 뒤
            section.markNeedsRegen(); // 분량/구조 미달 표식
        }
        return blockRepository.findBySectionIdOrderByOrderNoAsc(sectionId);
    }

    /** block contentJson(TipTap) 의 text 노드를 평탄화해 단어 수 추정 (공백 분리). */
    private int countWords(List<BlockInput> blocks) {
        StringBuilder sb = new StringBuilder();
        for (BlockInput b : blocks) {
            if (b.contentJson() != null) {
                collectText(b.contentJson(), sb);
            }
        }
        String text = sb.toString().trim();
        if (text.isEmpty()) return 0;
        return text.split("\\s+").length;
    }

    private void collectText(Object node, StringBuilder sb) {
        if (node instanceof Map<?, ?> map) {
            Object text = map.get("text");
            if (text instanceof String s) {
                sb.append(s).append(' ');
            }
            Object content = map.get("content");
            if (content instanceof List<?> list) {
                for (Object child : list) collectText(child, sb);
            }
        } else if (node instanceof List<?> list) {
            for (Object child : list) collectText(child, sb);
        }
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

    /** design 트리 일괄 저장 입력 (chapter 단위). */
    public record ChapterInput(
            String factorLabel,
            String factorTitle,
            String sourceSection,
            List<SectionInput> sections
    ) {}

    /** design 트리 일괄 저장 입력 (section 단위). */
    public record SectionInput(
            String subfactorLabel,
            String title,
            String scope,
            List<String> requirementRefs,
            Integer minWords
    ) {}
}
