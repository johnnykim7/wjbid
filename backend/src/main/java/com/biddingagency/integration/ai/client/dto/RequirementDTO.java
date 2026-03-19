package com.biddingagency.integration.ai.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

/**
 * Requirement DTO for document generation requests
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequirementDTO {

    /**
     * Requirement ID (optional)
     */
    private UUID id;

    /**
     * Category (DOCUMENT, FORMAT, SUBMISSION, DEADLINE, etc.)
     */
    private String category;

    /**
     * Requirement title
     */
    private String title;

    /**
     * Detailed description
     */
    private String description;

    /**
     * Is this a mandatory requirement?
     */
    private boolean isBlocker;

    /**
     * Additional metadata
     */
    private Map<String, Object> metadata;
}
