package com.biddingagency.domain.proposal.dto;

import com.biddingagency.domain.proposal.entity.ProposalBlock;
import com.biddingagency.domain.proposal.entity.ProposalChapter;
import com.biddingagency.domain.proposal.entity.ProposalSection;
import com.biddingagency.domain.proposal.entity.SectionStatus;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * CR-030 제안서 Section 트리 응답 DTO.
 *
 * 좌측 트리(chapter → section) 렌더용. block 본문은 section 선택 시 별도 조회({@link SectionDetail}).
 */
public record ProposalTreeDto(
        UUID documentId,
        List<ChapterNode> chapters
) {

    public record ChapterNode(
            UUID id,
            String factorLabel,
            String factorTitle,
            String sourceSection,
            int orderNo,
            List<SectionNode> sections
    ) {
        public static ChapterNode of(ProposalChapter c, List<SectionNode> sections) {
            return new ChapterNode(
                    c.getId(),
                    c.getFactorLabel(),
                    c.getFactorTitle(),
                    c.getSourceSection() != null ? c.getSourceSection().name() : null,
                    c.getOrderNo(),
                    sections
            );
        }
    }

    public record SectionNode(
            UUID id,
            UUID chapterId,
            String subfactorLabel,
            String title,
            SectionStatus status,
            boolean locked,
            Integer minWords,
            List<String> requirementRefs,
            int orderNo
    ) {
        public static SectionNode of(ProposalSection s) {
            return new SectionNode(
                    s.getId(),
                    s.getChapterId(),
                    s.getSubfactorLabel(),
                    s.getTitle(),
                    s.getStatus(),
                    s.isLocked(),
                    s.getMinWords(),
                    s.getRequirementRefs(),
                    s.getOrderNo()
            );
        }
    }

    /** section 선택 시 우측 패널 — 본문(block) + 메타. */
    public record SectionDetail(
            UUID id,
            String subfactorLabel,
            String title,
            String scope,
            SectionStatus status,
            boolean locked,
            Integer minWords,
            List<String> requirementRefs,
            List<BlockNode> blocks
    ) {
        public static SectionDetail of(ProposalSection s, List<ProposalBlock> blocks) {
            return new SectionDetail(
                    s.getId(),
                    s.getSubfactorLabel(),
                    s.getTitle(),
                    s.getScope(),
                    s.getStatus(),
                    s.isLocked(),
                    s.getMinWords(),
                    s.getRequirementRefs(),
                    blocks.stream().map(BlockNode::of).toList()
            );
        }
    }

    public record BlockNode(
            UUID id,
            String blockType,
            Map<String, Object> contentJson,
            Map<String, Object> sourceEvidence,
            int orderNo
    ) {
        public static BlockNode of(ProposalBlock b) {
            return new BlockNode(
                    b.getId(),
                    b.getBlockType() != null ? b.getBlockType().name() : null,
                    b.getContentJson(),
                    b.getSourceEvidence(),
                    b.getOrderNo()
            );
        }
    }
}
