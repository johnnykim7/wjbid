package com.biddingagency.integration.ai.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Response DTO from Python AI Service for document generation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentGenerationResponse {

    /**
     * Status of the generation (success, failed)
     */
    private String status;

    /**
     * Generated document in TipTap JSON format
     */
    private Map<String, Object> document;

    /**
     * Response metadata (tokens, duration, tools used, etc.)
     */
    private ResponseMetadata metadata;

    /**
     * Error message if status is failed
     */
    private String errorMessage;
}
