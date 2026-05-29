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

    // CR-019: "가져와야 함" 표식 첨부 수 (관리자 화면 표식) / 업로드 시 채울 후보 조회
    long countByOpportunityIdAndDownloadStatus(UUID opportunityId, AttachmentDownloadStatus status);

    List<OpportunityAttachment> findByOpportunityIdAndDownloadStatus(UUID opportunityId, AttachmentDownloadStatus status);
}
