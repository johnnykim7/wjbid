package com.biddingagency.integration.ai.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Single requirement item extracted by AI
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequirementItem {

    /**
     * Requirement category (DOCUMENT, FORMAT, SUBMISSION, DEADLINE, ELIGIBILITY, TECHNICAL, OTHER)
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
