package com.biddingagency.domain.compliance.repository;

import com.biddingagency.domain.compliance.entity.FulfillmentStatus;
import com.biddingagency.domain.compliance.entity.RequirementFulfillmentMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Requirement Fulfillment Map Repository
 */
@Repository
public interface RequirementFulfillmentMapRepository extends JpaRepository<RequirementFulfillmentMap, UUID> {

    /**
     * Find all mappings for a bid request
     */
    List<RequirementFulfillmentMap> findByBidRequestId(UUID bidRequestId);

    /**
     * Find by bid request and requirement
     */
    Optional<RequirementFulfillmentMap> findByBidRequestIdAndRequirementItemId(
            UUID bidRequestId, UUID requirementItemId);

    /**
     * Find by status
     */
    List<RequirementFulfillmentMap> findByBidRequestIdAndStatus(UUID bidRequestId, FulfillmentStatus status);

    /**
     * Count by status
     */
    long countByBidRequestIdAndStatus(UUID bidRequestId, FulfillmentStatus status);

    /**
     * Check if requirement is mapped
     */
    boolean existsByBidRequestIdAndRequirementItemId(UUID bidRequestId, UUID requirementItemId);

    /**
     * Get unfulfilled blocker requirements
     */
    @Query("SELECT m FROM RequirementFulfillmentMap m " +
            "JOIN FETCH m.requirementItem ri " +
            "WHERE m.bidRequest.id = :bidRequestId " +
            "AND ri.isBlocker = true " +
            "AND m.status = 'MISSING'")
    List<RequirementFulfillmentMap> findUnfulfilledBlockers(@Param("bidRequestId") UUID bidRequestId);

    /**
     * 공고 분석에서 사라진 요구 항목의 연결을 지운다 (CR-141, BIZ-045)
     */
    @Modifying
    @Query("DELETE FROM RequirementFulfillmentMap m WHERE m.requirementItem.id IN :requirementItemIds")
    int deleteByRequirementItemIds(@Param("requirementItemIds") Collection<UUID> requirementItemIds);

    /**
     * 한 제안서 문서의 절 충족 매핑을 지운다. 설계를 다시 저장하면 절 id 가 바뀌기 때문이다 (CR-159, BIZ-077)
     */
    @Modifying
    @Query("DELETE FROM RequirementFulfillmentMap m WHERE m.document.id = :documentId "
            + "AND m.fulfillmentType = com.biddingagency.domain.compliance.entity.FulfillmentType.DOCUMENT_SECTION")
    int deleteSectionMapsByDocumentId(@Param("documentId") UUID documentId);

    /**
     * 여러 입찰 건의 연결과 이어진 서류를 한 번에 (CR-141 제출 전 검사 목록)
     */
    @Query("SELECT m FROM RequirementFulfillmentMap m LEFT JOIN FETCH m.clientDocument "
            + "WHERE m.bidRequest.id IN :bidRequestIds")
    List<RequirementFulfillmentMap> findWithClientDocumentByBidRequestIds(
            @Param("bidRequestIds") Collection<UUID> bidRequestIds);

    /**
     * Delete all mappings for bid request
     */
    void deleteByBidRequestId(UUID bidRequestId);
}
