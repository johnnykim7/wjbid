package com.biddingagency.domain.compliance.service;

import com.biddingagency.domain.bid.entity.BidRequest;
import com.biddingagency.domain.bid.repository.BidRequestRepository;
import com.biddingagency.domain.compliance.dto.ComplianceIssue;
import com.biddingagency.domain.compliance.dto.ValidationResult;
import com.biddingagency.domain.compliance.entity.FulfillmentStatus;
import com.biddingagency.domain.compliance.entity.FulfillmentType;
import com.biddingagency.domain.compliance.entity.RequirementFulfillmentMap;
import com.biddingagency.domain.compliance.repository.RequirementFulfillmentMapRepository;
import com.biddingagency.domain.document.entity.BidDocument;
import com.biddingagency.domain.document.entity.DocumentStatus;
import com.biddingagency.domain.document.repository.BidDocumentRepository;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.entity.OpportunityRequirementItem;
import com.biddingagency.domain.opportunity.repository.OpportunityRequirementItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Compliance Service
 *
 * Core validation engine with BLOCKER detection system
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ComplianceService {

    private final BidRequestRepository bidRequestRepository;
    private final OpportunityRequirementItemRepository requirementRepository;
    private final RequirementFulfillmentMapRepository fulfillmentMapRepository;
    private final BidDocumentRepository documentRepository;

    /**
     * Validate bid request for submission readiness
     * This is the BLOCKER system - prevents submission if requirements not met
     */
    public ValidationResult validateBidRequest(UUID bidRequestId) {
        log.info("Validating bid request: {}", bidRequestId);

        BidRequest bidRequest = bidRequestRepository.findById(bidRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Bid request not found: " + bidRequestId));

        List<ComplianceIssue> blockers = new ArrayList<>();
        List<ComplianceIssue> warnings = new ArrayList<>();

        Opportunity opportunity = bidRequest.getOpportunity();

        // 1. Check deadline
        if (opportunity.getResponseDeadline() != null &&
                opportunity.getResponseDeadline().isBefore(LocalDateTime.now())) {
            blockers.add(ComplianceIssue.builder()
                    .severity(ComplianceIssue.Severity.BLOCKER)
                    .category("DEADLINE")
                    .message("Opportunity deadline has passed: " + opportunity.getResponseDeadline())
                    .suggestedAction("Cannot submit after deadline")
                    .build());
        }

        // 2. Check document status
        List<BidDocument> documents = documentRepository.findByBidRequestId(bidRequestId);
        for (BidDocument doc : documents) {
            if (doc.getStatus() == DocumentStatus.DRAFT) {
                warnings.add(ComplianceIssue.builder()
                        .severity(ComplianceIssue.Severity.WARNING)
                        .category("DOCUMENT_STATUS")
                        .message("Document " + doc.getDocumentType().getDisplayName() + " is still in DRAFT status")
                        .suggestedAction("Review and approve document")
                        .build());
            }
        }

        // 3. Check requirement fulfillment
        List<OpportunityRequirementItem> requirements =
                requirementRepository.findByOpportunityId(opportunity.getId());

        List<OpportunityRequirementItem> blockerRequirements =
                requirements.stream().filter(OpportunityRequirementItem::getIsBlocker).toList();

        int totalRequirements = blockerRequirements.size();
        int fulfilledCount = 0;

        for (OpportunityRequirementItem requirement : blockerRequirements) {
            RequirementFulfillmentMap fulfillment = fulfillmentMapRepository
                    .findByBidRequestIdAndRequirementItemId(bidRequestId, requirement.getId())
                    .orElse(null);

            if (fulfillment == null || fulfillment.getStatus() == FulfillmentStatus.MISSING) {
                blockers.add(ComplianceIssue.builder()
                        .severity(ComplianceIssue.Severity.BLOCKER)
                        .category("REQUIREMENT")
                        .requirementItemId(requirement.getId())
                        .requirementTitle(requirement.getTitle())
                        .message("Required item not fulfilled: " + requirement.getTitle())
                        .suggestedAction("Map requirement to document section or attachment")
                        .build());
            } else if (fulfillment.getStatus() == FulfillmentStatus.FULFILLED) {
                fulfilledCount++;
            }
        }

        // Calculate fulfillment rate
        double fulfillmentRate = totalRequirements > 0 ?
                (double) fulfilledCount / totalRequirements * 100 : 100.0;

        // Determine overall status
        ValidationResult.Status status;
        if (!blockers.isEmpty()) {
            status = ValidationResult.Status.BLOCKER;
        } else if (!warnings.isEmpty()) {
            status = ValidationResult.Status.WARNING;
        } else {
            status = ValidationResult.Status.PASS;
        }

        ValidationResult result = ValidationResult.builder()
                .status(status)
                .blockers(blockers)
                .warnings(warnings)
                .fulfillmentRate(fulfillmentRate)
                .totalRequirements(totalRequirements)
                .fulfilledRequirements(fulfilledCount)
                .validatedAt(LocalDateTime.now())
                .build();

        log.info("Validation complete: status={}, blockers={}, warnings={}, fulfillmentRate={}%",
                status, blockers.size(), warnings.size(), fulfillmentRate);

        return result;
    }

    /**
     * Get blocker issues only
     */
    public List<ComplianceIssue> getBlockers(UUID bidRequestId) {
        ValidationResult result = validateBidRequest(bidRequestId);
        return result.getBlockers();
    }

    /**
     * Create or update fulfillment mapping
     */
    @Transactional
    public RequirementFulfillmentMap mapRequirement(UUID bidRequestId, UUID requirementItemId,
                                                     FulfillmentType fulfillmentType,
                                                     UUID documentId, String sectionPath,
                                                     String attachmentRef, String notes,
                                                     UUID mappedBy) {
        log.info("Mapping requirement {} for bid request {}", requirementItemId, bidRequestId);

        BidRequest bidRequest = bidRequestRepository.findById(bidRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Bid request not found: " + bidRequestId));

        OpportunityRequirementItem requirement = requirementRepository.findById(requirementItemId)
                .orElseThrow(() -> new IllegalArgumentException("Requirement not found: " + requirementItemId));

        BidDocument document = null;
        if (documentId != null) {
            document = documentRepository.findById(documentId).orElse(null);
        }

        // Find existing or create new
        RequirementFulfillmentMap map = fulfillmentMapRepository
                .findByBidRequestIdAndRequirementItemId(bidRequestId, requirementItemId)
                .orElse(RequirementFulfillmentMap.builder()
                        .bidRequest(bidRequest)
                        .requirementItem(requirement)
                        .build());

        // Update fulfillment details
        map.updateFulfillment(fulfillmentType, documentId, sectionPath, attachmentRef, notes, mappedBy);

        if (document != null) {
            map.setDocument(document);
        }

        RequirementFulfillmentMap saved = fulfillmentMapRepository.save(map);

        log.info("Requirement mapping saved: {}", saved.getId());

        return saved;
    }

    /**
     * Get all fulfillment maps for bid request
     */
    public List<RequirementFulfillmentMap> getFulfillmentMaps(UUID bidRequestId) {
        return fulfillmentMapRepository.findByBidRequestId(bidRequestId);
    }

    /**
     * Generate compliance matrix (requirement → fulfillment table)
     */
    public List<ComplianceMatrixRow> generateComplianceMatrix(UUID bidRequestId) {
        BidRequest bidRequest = bidRequestRepository.findByIdWithDetails(bidRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Bid request not found: " + bidRequestId));

        List<OpportunityRequirementItem> requirements =
                requirementRepository.findByOpportunityId(bidRequest.getOpportunity().getId());

        List<ComplianceMatrixRow> matrix = new ArrayList<>();

        for (OpportunityRequirementItem requirement : requirements) {
            RequirementFulfillmentMap map = fulfillmentMapRepository
                    .findByBidRequestIdAndRequirementItemId(bidRequestId, requirement.getId())
                    .orElse(null);

            ComplianceMatrixRow row = ComplianceMatrixRow.builder()
                    .requirementId(requirement.getId())
                    .requirement(requirement.getTitle())
                    .category(requirement.getCategory().name())
                    .mandatory(requirement.getIsBlocker() ? "Yes" : "No")
                    .response(map != null ? map.getNotes() : "Not addressed")
                    .documentSection(map != null ? map.getDocumentSectionPath() : "N/A")
                    .status(map != null ? map.getStatus().name() : "PENDING")
                    .build();

            matrix.add(row);
        }

        return matrix;
    }

    /**
     * Compliance Matrix Row DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class ComplianceMatrixRow {
        private UUID requirementId;
        private String requirement;
        private String category;
        private String mandatory;
        private String response;
        private String documentSection;
        private String status;
    }
}
