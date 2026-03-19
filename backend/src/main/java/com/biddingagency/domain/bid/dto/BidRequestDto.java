package com.biddingagency.domain.bid.dto;

import com.biddingagency.domain.bid.entity.BidRequest;
import com.biddingagency.domain.bid.entity.StateTransition;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.format.DateTimeFormatter;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BidRequestDto(
        String id,
        String opportunityId,
        String opportunityTitle,
        String solicitationNumber,
        String agencyName,
        String state,
        String stateDisplay,
        String createdAt,
        String updatedAt,
        String submittedAt,
        List<StateTransition> history
) {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static BidRequestDto from(BidRequest br) {
        return new BidRequestDto(
                br.getId() != null ? br.getId().toString() : null,
                br.getOpportunity() != null ? br.getOpportunity().getId().toString() : null,
                br.getOpportunity() != null ? br.getOpportunity().getTitle() : null,
                br.getOpportunity() != null ? br.getOpportunity().getSolicitationNumber() : null,
                br.getOpportunity() != null ? br.getOpportunity().getOrganizationName() : null,
                br.getState() != null ? br.getState().name() : null,
                br.getStateDisplay(),
                br.getCreatedAt() != null ? br.getCreatedAt().format(FMT) : null,
                br.getUpdatedAt() != null ? br.getUpdatedAt().format(FMT) : null,
                br.getSubmittedAt() != null ? br.getSubmittedAt().format(FMT) : null,
                null
        );
    }

    public static BidRequestDto withHistory(BidRequest br) {
        return new BidRequestDto(
                br.getId() != null ? br.getId().toString() : null,
                br.getOpportunity() != null ? br.getOpportunity().getId().toString() : null,
                br.getOpportunity() != null ? br.getOpportunity().getTitle() : null,
                br.getOpportunity() != null ? br.getOpportunity().getSolicitationNumber() : null,
                br.getOpportunity() != null ? br.getOpportunity().getOrganizationName() : null,
                br.getState() != null ? br.getState().name() : null,
                br.getStateDisplay(),
                br.getCreatedAt() != null ? br.getCreatedAt().format(FMT) : null,
                br.getUpdatedAt() != null ? br.getUpdatedAt().format(FMT) : null,
                br.getSubmittedAt() != null ? br.getSubmittedAt().format(FMT) : null,
                br.getStateHistory()
        );
    }
}
