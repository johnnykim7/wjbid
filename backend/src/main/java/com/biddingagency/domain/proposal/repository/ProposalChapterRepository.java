package com.biddingagency.domain.proposal.repository;

import com.biddingagency.domain.proposal.entity.ProposalChapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProposalChapterRepository extends JpaRepository<ProposalChapter, UUID> {

    List<ProposalChapter> findByDocumentIdOrderByOrderNoAsc(UUID documentId);

    long countByDocumentId(UUID documentId);

    void deleteByDocumentId(UUID documentId);
}
