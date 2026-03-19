package com.biddingagency.domain.document.repository;

import com.biddingagency.domain.document.entity.BidDocument;
import com.biddingagency.domain.document.entity.DocumentStatus;
import com.biddingagency.domain.document.entity.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Bid Document Repository
 */
@Repository
public interface BidDocumentRepository extends JpaRepository<BidDocument, UUID> {

    /**
     * Find all documents for a bid request
     */
    List<BidDocument> findByBidRequestId(UUID bidRequestId);

    /**
     * Find by bid request and document type
     */
    Optional<BidDocument> findByBidRequestIdAndDocumentType(UUID bidRequestId, DocumentType documentType);

    /**
     * Find by status
     */
    List<BidDocument> findByStatus(DocumentStatus status);

    /**
     * Find by bid request and status
     */
    List<BidDocument> findByBidRequestIdAndStatus(UUID bidRequestId, DocumentStatus status);

    /**
     * Check if document exists for bid request and type
     */
    boolean existsByBidRequestIdAndDocumentType(UUID bidRequestId, DocumentType documentType);

    /**
     * Count documents by bid request
     */
    long countByBidRequestId(UUID bidRequestId);
}
