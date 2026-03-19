package com.biddingagency.dto;

import com.biddingagency.domain.bid.entity.BidRequestState;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * State transition request DTO
 */
@Data
public class StateTransitionRequest {

    @NotNull(message = "Target state is required")
    private BidRequestState toState;

    private String notes;
}
