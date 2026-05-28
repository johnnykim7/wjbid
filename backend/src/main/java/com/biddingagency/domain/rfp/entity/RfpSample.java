package com.biddingagency.domain.rfp.entity;

import com.biddingagency.common.BaseEntity;
import com.biddingagency.common.JsonMapConverter;
import jakarta.persistence.*;
import lombok.*;

import java.util.Map;

/** 성공 제안서 (CR-013) — 등록 단위. 메타 + 원본 파일들 + (선택)PWS. */
@Entity
@Table(name = "rfp_sample",
        indexes = {
                @Index(name = "idx_rfp_sample_industry", columnList = "industry_type"),
                @Index(name = "idx_rfp_sample_outcome", columnList = "outcome")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RfpSample extends BaseEntity {

    @Column(name = "opportunity_no", nullable = false, length = 50)
    private String opportunityNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "industry_type", nullable = false, length = 30)
    private IndustryType industryType;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 20)
    private Outcome outcome;

    @Column(name = "company", length = 200)
    private String company;

    @Column(name = "agency", length = 200)
    private String agency;

    @Column(name = "award_amount")
    private Long awardAmount;

    @Column(name = "fiscal_year")
    private Integer fiscalYear;

    @Column(name = "use_for_pattern", nullable = false)
    @Builder.Default
    private boolean useForPattern = true;

    /** 보유 FACTOR 구성 (공고마다 가변) */
    @Convert(converter = JsonMapConverter.class)
    @Column(name = "factors_json", columnDefinition = "JSON")
    private Map<String, Object> factorsJson;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;
}
