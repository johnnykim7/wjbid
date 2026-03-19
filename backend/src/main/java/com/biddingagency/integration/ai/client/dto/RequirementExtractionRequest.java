package com.biddingagency.integration.ai.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

/**
 * Request DTO for requirement extraction from Python AI Service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequirementExtractionRequest {

    /**
     * Opportunity UUID
     */
    private UUID opportunityId;

    /**
     * Full opportunity text from SAM.gov
     */
    private String opportunityText;

    /**
     * Additional metadata
     */
    @Builder.Default
    private Map<String, Object> metadata = Map.of();
}
