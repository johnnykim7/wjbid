package com.biddingagency.domain.document.service;

import com.biddingagency.domain.bid.entity.BidRequest;
import com.biddingagency.domain.bid.repository.BidRequestRepository;
import com.biddingagency.domain.document.entity.BidDocument;
import com.biddingagency.domain.document.entity.BidDocumentVersion;
import com.biddingagency.domain.document.entity.DocumentStatus;
import com.biddingagency.domain.document.entity.DocumentType;
import com.biddingagency.domain.document.repository.BidDocumentRepository;
import com.biddingagency.domain.document.repository.BidDocumentVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Document Version Service
 *
 * Handles immutable document versioning and LOCKED state enforcement
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocumentVersionService {

    private final BidDocumentRepository documentRepository;
    private final BidDocumentVersionRepository versionRepository;
    private final BidRequestRepository bidRequestRepository;

    /**
     * Create new document
     */
    @Transactional
    public BidDocument createDocument(UUID bidRequestId, DocumentType documentType,
                                       Map<String, Object> initialContent, UUID createdBy) {
        log.info("Creating document {} for bid request {}", documentType, bidRequestId);

        // Check if already exists
        if (documentRepository.existsByBidRequestIdAndDocumentType(bidRequestId, documentType)) {
            throw new IllegalStateException("Document of type " + documentType + " already exists for this bid request");
        }

        // Load bid request
        BidRequest bidRequest = bidRequestRepository.findById(bidRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Bid request not found: " + bidRequestId));

        // Create document
        BidDocument document = BidDocument.builder()
                .bidRequest(bidRequest)
                .documentType(documentType)
                .status(DocumentStatus.DRAFT)
                .currentVersionNo(1)
                .build();

        BidDocument savedDocument = documentRepository.save(document);

        // Create initial version
        BidDocumentVersion version = BidDocumentVersion.builder()
                .document(savedDocument)
                .versionNo(1)
                .contentJson(initialContent)
                .editedBy(createdBy)
                .editedAt(LocalDateTime.now())
                .changeSummary("Initial version")
                .wordCount(BidDocumentVersion.calculateWordCount(initialContent))
                .build();

        versionRepository.save(version);

        log.info("Document created: {}, version 1", savedDocument.getId());

        return savedDocument;
    }

    /**
     * Save new version
     * Enforces LOCKED state - cannot edit locked documents
     */
    @Transactional
    public BidDocumentVersion saveVersion(UUID documentId, Map<String, Object> contentJson,
                                           String changeSummary, UUID editedBy) {
        log.info("Saving new version for document {}", documentId);

        BidDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        // CRITICAL: Enforce LOCKED state
        if (document.isLocked()) {
            throw new IllegalStateException(
                    "Cannot edit LOCKED document. Create amendment instead using createAmendment()");
        }

        // Increment version
        int newVersionNo = document.getCurrentVersionNo() + 1;
        document.incrementVersion();

        // Create new version (immutable)
        BidDocumentVersion version = BidDocumentVersion.builder()
                .document(document)
                .versionNo(newVersionNo)
                .contentJson(contentJson)
                .editedBy(editedBy)
                .editedAt(LocalDateTime.now())
                .changeSummary(changeSummary)
                .wordCount(BidDocumentVersion.calculateWordCount(contentJson))
                .build();

        BidDocumentVersion saved = versionRepository.save(version);
        documentRepository.save(document);

        log.info("Version {} saved for document {}", newVersionNo, documentId);

        return saved;
    }

    /**
     * Get latest version
     */
    public BidDocumentVersion getLatestVersion(UUID documentId) {
        return versionRepository.findLatestVersion(documentId)
                .orElseThrow(() -> new IllegalArgumentException("No versions found for document: " + documentId));
    }

    /**
     * Get specific version
     */
    public BidDocumentVersion getVersion(UUID documentId, int versionNo) {
        return versionRepository.findByDocumentIdAndVersionNo(documentId, versionNo)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Version " + versionNo + " not found for document: " + documentId));
    }

    /**
     * Get all versions (descending order)
     */
    public List<BidDocumentVersion> getAllVersions(UUID documentId) {
        return versionRepository.findByDocumentIdOrderByVersionNoDesc(documentId);
    }

    /**
     * Rollback to previous version
     * Creates new version with content from old version
     */
    @Transactional
    public BidDocumentVersion rollbackToVersion(UUID documentId, int targetVersionNo,
                                                  UUID rolledBackBy) {
        log.info("Rolling back document {} to version {}", documentId, targetVersionNo);

        BidDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        // Cannot rollback locked document
        if (document.isLocked()) {
            throw new IllegalStateException("Cannot rollback LOCKED document");
        }

        // Get target version
        BidDocumentVersion targetVersion = getVersion(documentId, targetVersionNo);

        // Save as new version with rollback note
        String changeSummary = "Rolled back to version " + targetVersionNo;

        return saveVersion(documentId, targetVersion.getContentJson(), changeSummary, rolledBackBy);
    }

    /**
     * Lock document (make immutable)
     * Called when bid is submitted
     */
    @Transactional
    public void lockDocument(UUID documentId) {
        log.info("Locking document {}", documentId);

        BidDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        document.lock();
        documentRepository.save(document);

        log.info("Document {} is now LOCKED (immutable)", documentId);
    }

    /**
     * Create amendment (for post-submission corrections)
     * Copies LOCKED document to new DRAFT document
     */
    @Transactional
    public BidDocument createAmendment(UUID lockedDocumentId, UUID createdBy) {
        log.info("Creating amendment for locked document {}", lockedDocumentId);

        BidDocument original = documentRepository.findById(lockedDocumentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + lockedDocumentId));

        if (!original.isLocked()) {
            throw new IllegalStateException("Can only create amendments for LOCKED documents");
        }

        // Get latest version content
        BidDocumentVersion latestVersion = getLatestVersion(lockedDocumentId);

        // Create new document with same type
        BidDocument newDocument = BidDocument.builder()
                .bidRequest(original.getBidRequest())
                .documentType(original.getDocumentType())
                .status(DocumentStatus.DRAFT)
                .currentVersionNo(1)
                .build();

        BidDocument savedDocument = documentRepository.save(newDocument);

        // Create first version as copy of original
        BidDocumentVersion amendmentVersion = BidDocumentVersion.builder()
                .document(savedDocument)
                .versionNo(1)
                .contentJson(latestVersion.getContentJson())
                .editedBy(createdBy)
                .editedAt(LocalDateTime.now())
                .changeSummary("Amendment created from document " + lockedDocumentId)
                .wordCount(latestVersion.getWordCount())
                .build();

        versionRepository.save(amendmentVersion);

        log.info("Amendment document created: {}", savedDocument.getId());

        return savedDocument;
    }

    /**
     * Get all documents for a bid request with their latest version content
     */
    public List<java.util.Map<String, Object>> getDocumentSummaries(UUID bidRequestId) {
        List<BidDocument> docs = documentRepository.findByBidRequestId(bidRequestId);
        return docs.stream().map(doc -> {
            java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
            result.put("id", doc.getId() != null ? doc.getId().toString() : null);
            result.put("documentType", doc.getDocumentType() != null ? doc.getDocumentType().name() : null);
            result.put("status", doc.getStatus() != null ? doc.getStatus().name() : null);
            result.put("currentVersionNo", doc.getCurrentVersionNo());
            result.put("createdAt", doc.getCreatedAt() != null ? doc.getCreatedAt().toString() : null);
            versionRepository.findLatestVersion(doc.getId()).ifPresent(v -> {
                result.put("contentJson", v.getContentJson());
                result.put("changeSummary", v.getChangeSummary());
                result.put("editedAt", v.getEditedAt() != null ? v.getEditedAt().toString() : null);
            });
            return result;
        }).toList();
    }

    /**
     * Update document status
     */
    @Transactional
    public BidDocument updateStatus(UUID documentId, DocumentStatus newStatus) {
        log.info("Updating document {} status to {}", documentId, newStatus);

        BidDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        document.updateStatus(newStatus);

        return documentRepository.save(document);
    }
}
