package com.biddingagency.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

/**
 * Bid request creation request DTO
 */
@Data
public class BidRequestCreateRequest {

    @NotNull(message = "Opportunity ID is required")
    private UUID opportunityId;
}
