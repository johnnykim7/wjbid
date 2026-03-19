package com.biddingagency.domain.document.repository;

import com.biddingagency.domain.document.entity.BidDocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Bid Document Version Repository
 */
@Repository
public interface BidDocumentVersionRepository extends JpaRepository<BidDocumentVersion, UUID> {

    /**
     * Find all versions for a document (ordered by version number desc)
     */
    List<BidDocumentVersion> findByDocumentIdOrderByVersionNoDesc(UUID documentId);

    /**
     * Find specific version
     */
    Optional<BidDocumentVersion> findByDocumentIdAndVersionNo(UUID documentId, Integer versionNo);

    /**
     * Get latest version
     */
    @Query("SELECT v FROM BidDocumentVersion v WHERE v.document.id = :documentId ORDER BY v.versionNo DESC LIMIT 1")
    Optional<BidDocumentVersion> findLatestVersion(@Param("documentId") UUID documentId);

    /**
     * Count versions for document
     */
    long countByDocumentId(UUID documentId);

    /**
     * Delete all versions for document
     */
    void deleteByDocumentId(UUID documentId);
}
