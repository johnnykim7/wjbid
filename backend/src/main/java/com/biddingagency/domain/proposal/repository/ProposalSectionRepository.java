package com.biddingagency.domain.proposal.repository;

import com.biddingagency.domain.proposal.entity.ProposalSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProposalSectionRepository extends JpaRepository<ProposalSection, UUID> {

    List<ProposalSection> findByChapterIdOrderByOrderNoAsc(UUID chapterId);

    /** 한 document 의 모든 section (chapter join) — 트리 조회·요구사항 매핑용 */
    @Query("SELECT s FROM ProposalSection s WHERE s.chapterId IN " +
            "(SELECT c.id FROM ProposalChapter c WHERE c.documentId = :documentId) " +
            "ORDER BY s.orderNo ASC")
    List<ProposalSection> findAllByDocumentId(@Param("documentId") UUID documentId);

    void deleteByChapterId(UUID chapterId);
}
