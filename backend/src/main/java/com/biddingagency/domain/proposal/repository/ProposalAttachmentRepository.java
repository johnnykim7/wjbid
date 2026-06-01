package com.biddingagency.domain.proposal.repository;

import com.biddingagency.domain.proposal.entity.ProposalAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProposalAttachmentRepository extends JpaRepository<ProposalAttachment, UUID> {

    List<ProposalAttachment> findByDocumentIdOrderByCreatedAtAsc(UUID documentId);
}
