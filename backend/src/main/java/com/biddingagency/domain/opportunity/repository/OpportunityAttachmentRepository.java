package com.biddingagency.domain.opportunity.repository;

import com.biddingagency.domain.opportunity.entity.AttachmentDownloadStatus;
import com.biddingagency.domain.opportunity.entity.OpportunityAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OpportunityAttachmentRepository extends JpaRepository<OpportunityAttachment, UUID> {

    List<OpportunityAttachment> findByOpportunityId(UUID opportunityId);

    List<OpportunityAttachment> findByDownloadStatus(AttachmentDownloadStatus status);

    long countByOpportunityId(UUID opportunityId);
}
