package com.biddingagency.domain.federal.entity;

import com.biddingagency.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * USAspending 에서 받은 한국 내 국방부 계약 (CR-142, BID-MKT-025).
 * 우리 공고(opportunities)와 섞지 않는다. 계약 구분은 저장하지 않고 조회할 때 낸다(BIZ-050).
 */
@Entity
@Table(name = "federal_contracts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FederalContract extends BaseEntity {

    @Column(name = "award_key", nullable = false, length = 200)
    private String awardKey;

    @Column(name = "piid", nullable = false, length = 100)
    private String piid;

    @Column(name = "parent_piid", length = 100)
    private String parentPiid;

    @Enumerated(EnumType.STRING)
    @Column(name = "award_type", nullable = false, length = 30)
    private FederalAwardType awardType;

    @Column(name = "recipient_name", length = 300)
    private String recipientName;

    @Column(name = "recipient_uei", length = 20)
    private String recipientUei;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    /** 옵션 끝날짜. 상세에서 받는다(BID-MKT-026) */
    @Column(name = "potential_end_date")
    private LocalDate potentialEndDate;

    @Column(name = "solicitation_number", length = 100)
    private String solicitationNumber;

    /** 상세를 받은 시각(Asia/Seoul). 비어 있으면 받아야 한다 */
    @Column(name = "detail_fetched_at")
    private LocalDateTime detailFetchedAt;

    @Column(name = "amount_usd", precision = 15, scale = 2)
    private BigDecimal amountUsd;

    @Column(name = "naics_code", length = 10)
    private String naicsCode;

    @Column(name = "naics_name", length = 300)
    private String naicsName;

    @Column(name = "psc_code", length = 10)
    private String pscCode;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "awarding_sub_agency", length = 200)
    private String awardingSubAgency;

    /** USAspending 의 마지막 바뀐 날. 매일 받기의 기준 */
    @Column(name = "source_modified_date")
    private LocalDate sourceModifiedDate;

    /** 검색 응답 한 줄 원본(search), 상세를 받았으면 상세의 필요한 칸 원본(detail) (BIZ-052) */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_json", columnDefinition = "JSON", nullable = false)
    private Map<String, Object> rawJson;

    /**
     * 같은 계약 고유 키로 받은 검색 결과로 덮어쓴다(BIZ-052).
     * 마지막 바뀐 날이 저장된 것보다 뒤이면 옵션 끝날짜를 다시 받도록 상세 받은 때를 비운다.
     */
    public void overwrite(FederalContract incoming) {
        boolean modified = incoming.sourceModifiedDate != null
                && (sourceModifiedDate == null || incoming.sourceModifiedDate.isAfter(sourceModifiedDate));
        this.piid = incoming.piid;
        this.parentPiid = incoming.parentPiid;
        this.awardType = incoming.awardType;
        this.recipientName = incoming.recipientName;
        this.recipientUei = incoming.recipientUei;
        this.startDate = incoming.startDate;
        this.endDate = incoming.endDate;
        this.amountUsd = incoming.amountUsd;
        this.naicsCode = incoming.naicsCode;
        this.naicsName = incoming.naicsName;
        this.pscCode = incoming.pscCode;
        this.description = incoming.description;
        this.awardingSubAgency = incoming.awardingSubAgency;
        this.sourceModifiedDate = incoming.sourceModifiedDate;
        Map<String, Object> raw = rawJson == null ? new HashMap<>() : new HashMap<>(rawJson);
        raw.put("search", incoming.rawJson.get("search"));
        this.rawJson = raw;
        if (modified) this.detailFetchedAt = null;
    }

    /** 상세에서 받은 옵션 끝날짜와 원래 공고번호를 넣는다(BID-MKT-026) */
    public void applyDetail(LocalDate potentialEndDate, String solicitationNumber, Map<String, Object> detailRaw,
                            LocalDateTime fetchedAt) {
        this.potentialEndDate = potentialEndDate;
        this.solicitationNumber = solicitationNumber;
        this.detailFetchedAt = fetchedAt;
        Map<String, Object> raw = rawJson == null ? new HashMap<>() : new HashMap<>(rawJson);
        raw.put("detail", detailRaw);
        this.rawJson = raw;
    }

    /** 회원 계약이 따라가는 두 날짜가 같은지 */
    public boolean sameDates(LocalDate end, LocalDate potentialEnd) {
        return Objects.equals(endDate, end) && Objects.equals(potentialEndDate, potentialEnd);
    }
}
