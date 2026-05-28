package com.biddingagency.domain.rfp.dto;

import com.biddingagency.domain.rfp.entity.IndustryType;
import com.biddingagency.domain.rfp.entity.Outcome;

/** 성공 제안서 메타 등록 요청 (CR-013) */
public record RfpSampleCreateRequest(
        String opportunityNo,
        IndustryType industryType,
        Outcome outcome,
        String company,
        String agency,
        Long awardAmount,
        Integer fiscalYear,
        String note
) {}
