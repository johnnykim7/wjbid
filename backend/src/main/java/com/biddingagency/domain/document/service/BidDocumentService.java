package com.biddingagency.domain.document.service;

import com.biddingagency.domain.document.entity.BidDocument;
import com.biddingagency.domain.document.entity.DocumentStatus;
import com.biddingagency.domain.document.entity.DocumentType;
import com.biddingagency.domain.document.repository.BidDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Bid Document Service
 *
 * High-level document operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BidDocumentService {

    private final BidDocumentRepository documentRepository;

    /**
     * Find document by ID
     */
    public BidDocument findById(UUID id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + id));
    }

    /**
     * Find all documents for bid request
     */
    public List<BidDocument> findByBidRequest(UUID bidRequestId) {
        return documentRepository.findByBidRequestId(bidRequestId);
    }

    /**
     * Find by bid request and document type
     */
    public BidDocument findByBidRequestAndType(UUID bidRequestId, DocumentType documentType) {
        return documentRepository.findByBidRequestIdAndDocumentType(bidRequestId, documentType)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Document of type " + documentType + " not found for bid request " + bidRequestId));
    }

    /**
     * Find by status
     */
    public List<BidDocument> findByStatus(DocumentStatus status) {
        return documentRepository.findByStatus(status);
    }

    /**
     * Count documents for bid request
     */
    public long countByBidRequest(UUID bidRequestId) {
        return documentRepository.countByBidRequestId(bidRequestId);
    }

    /**
     * Lock all documents for bid request (called when submitting)
     */
    @Transactional
    public void lockAllDocuments(UUID bidRequestId) {
        log.info("Locking all documents for bid request {}", bidRequestId);

        List<BidDocument> documents = findByBidRequest(bidRequestId);

        for (BidDocument document : documents) {
            if (!document.isLocked()) {
                document.lock();
                documentRepository.save(document);
            }
        }

        log.info("Locked {} documents", documents.size());
    }
}
