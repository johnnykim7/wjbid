package com.biddingagency.domain.rfp.dto;

/** 슬롯 배치 요청 (CR-013). sampleFileId 또는 sectionText 중 하나. */
public record SlotAssignRequest(
        String sampleFileId,
        String sectionText,
        String otherLabel,
        Boolean confirmed
) {}
