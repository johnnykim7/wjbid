package com.biddingagency.integration.ai.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Request DTO for document generation from Python AI Service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentGenerationRequest {

    /**
     * Bid Request UUID
     */
    private UUID bidRequestId;

    /**
     * Document type (TECHNICAL_PROPOSAL, COVER_LETTER, PRICE_PROPOSAL, etc.)
     */
    private String documentType;

    /**
     * Full opportunity text for context
     */
    private String opportunityText;

    /**
     * List of requirements to address in the document
     */
    private List<RequirementDTO> requirements;

    /**
     * Additional context for document generation
     */
    @Builder.Default
    private Map<String, Object> context = Map.of();
}
