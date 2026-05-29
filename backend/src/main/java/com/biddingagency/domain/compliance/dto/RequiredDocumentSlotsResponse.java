package com.biddingagency.domain.compliance.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 요구사항 슬롯 목록 응답 (CR-010)
 */
@Getter
@Builder
public class RequiredDocumentSlotsResponse {

    private List<RequiredDocumentSlot> data;
    private Summary summary;

    @Getter
    @Builder
    public static class Summary {
        private int totalBlocker;
        private int fulfilledBlocker;
        private boolean canTransitionToDocsReceived;
    }
}
