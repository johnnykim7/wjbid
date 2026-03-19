package com.biddingagency.integration.ai.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Metadata about AI service response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseMetadata {

    /**
     * Number of tokens used
     */
    private int tokensUsed;

    /**
     * Duration in milliseconds
     */
    private long durationMs;

    /**
     * Tools/functions called during execution
     */
    private List<String> toolsCalled;

    /**
     * Model used (e.g., gpt-4, claude-3-5-sonnet)
     */
    private String model;
}
