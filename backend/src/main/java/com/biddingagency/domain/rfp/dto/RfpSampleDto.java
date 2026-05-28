package com.biddingagency.domain.rfp.dto;

import com.biddingagency.domain.rfp.entity.RfpSample;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.format.DateTimeFormatter;

/** 성공 제안서 목록/요약 DTO (CR-013) */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RfpSampleDto(
        String id,
        String opportunityNo,
        String industryType,
        String outcome,
        String company,
        String agency,
        Long awardAmount,
        Integer fiscalYear,
        boolean useForPattern,
        int fileCount,
        int assignedSlotCount,
        String createdAt
) {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static RfpSampleDto from(RfpSample s, int fileCount, int assignedSlotCount) {
        return new RfpSampleDto(
                s.getId().toString(),
                s.getOpportunityNo(),
                s.getIndustryType() != null ? s.getIndustryType().name() : null,
                s.getOutcome() != null ? s.getOutcome().name() : null,
                s.getCompany(),
                s.getAgency(),
                s.getAwardAmount(),
                s.getFiscalYear(),
                s.isUseForPattern(),
                fileCount,
                assignedSlotCount,
                s.getCreatedAt() != null ? s.getCreatedAt().format(FMT) : null
        );
    }
}
