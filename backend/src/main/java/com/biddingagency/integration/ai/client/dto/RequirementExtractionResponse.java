package com.biddingagency.integration.ai.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO from Python AI Service for requirement extraction
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequirementExtractionResponse {

    /**
     * Status of the extraction (success, failed)
     */
    private String status;

    /**
     * Extracted requirements
     */
    private List<RequirementItem> requirements;

    /**
     * Response metadata (tokens, duration, etc.)
     */
    private ResponseMetadata metadata;

    /**
     * Error message if status is failed
     */
    private String errorMessage;
}
