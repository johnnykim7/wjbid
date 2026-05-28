package com.biddingagency.domain.rfp.dto;

import com.biddingagency.domain.rfp.entity.RfpSample;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/** 성공 제안서 상세 DTO (CR-013) — 메타 + 파일 + 슬롯 배치 현황(빈슬롯 포함) */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RfpSampleDetailDto(
        String id,
        String opportunityNo,
        String industryType,
        String outcome,
        String company,
        String agency,
        Long awardAmount,
        Integer fiscalYear,
        boolean useForPattern,
        Map<String, Object> factorsJson,
        String note,
        String createdAt,
        List<RfpSampleFileDto> files,
        List<SlotStatusDto> slots
) {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static RfpSampleDetailDto of(RfpSample s, List<RfpSampleFileDto> files, List<SlotStatusDto> slots) {
        return new RfpSampleDetailDto(
                s.getId().toString(),
                s.getOpportunityNo(),
                s.getIndustryType() != null ? s.getIndustryType().name() : null,
                s.getOutcome() != null ? s.getOutcome().name() : null,
                s.getCompany(),
                s.getAgency(),
                s.getAwardAmount(),
                s.getFiscalYear(),
                s.isUseForPattern(),
                s.getFactorsJson(),
                s.getNote(),
                s.getCreatedAt() != null ? s.getCreatedAt().format(FMT) : null,
                files,
                slots
        );
    }
}
