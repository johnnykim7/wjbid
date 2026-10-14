package com.biddingagency.domain.opportunity.repository;

import com.biddingagency.domain.opportunity.entity.AttachmentDownloadStatus;
import com.biddingagency.domain.opportunity.entity.OpportunityAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OpportunityAttachmentRepository extends JpaRepository<OpportunityAttachment, UUID> {

    List<OpportunityAttachment> findByOpportunityId(UUID opportunityId);

    List<OpportunityAttachment> findByDownloadStatus(AttachmentDownloadStatus status);

    long countByOpportunityId(UUID opportunityId);

    // CR-019: "가져와야 함" 표식 첨부 수 (관리자 화면 표식) / 업로드 시 채울 후보 조회
    long countByOpportunityIdAndDownloadStatus(UUID opportunityId, AttachmentDownloadStatus status);

    List<OpportunityAttachment> findByOpportunityIdAndDownloadStatus(UUID opportunityId, AttachmentDownloadStatus status);

    /**
     * CR-160: 같은 공고번호의 다른 판에 같은 내용으로 저장된 SAM 또는 PIEE 첨부(BIZ-080).
     * 손으로 올린 첨부의 저장 파일은 관리자가 지울 수 있어 뺀다.
     */
    @Query("SELECT a FROM OpportunityAttachment a WHERE a.contentSha256 = :sha256 "
            + "AND a.downloadStatus = :status AND a.storageUrl IS NOT NULL AND a.sourceUrl <> :excludedSource "
            + "AND TRIM(a.opportunity.solicitationNumber) = :solNo AND a.opportunity.id <> :opportunityId")
    List<OpportunityAttachment> findSharable(@Param("sha256") String sha256,
                                             @Param("solNo") String solicitationNumber,
                                             @Param("opportunityId") UUID opportunityId,
                                             @Param("status") AttachmentDownloadStatus status,
                                             @Param("excludedSource") String excludedSource);
}
