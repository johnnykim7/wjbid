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

import java.time.LocalDate;
import java.util.UUID;

/**
 * 회원이 끝날짜 알림을 받으려고 등록한 계약 (CR-142, BID-MKT-031).
 * 알림 이력은 notification_logs 에 둔다(reference_type MEMBER_CONTRACT).
 */
@Entity
@Table(name = "member_contracts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MemberContract extends BaseEntity {

    @Column(name = "member_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID memberId;

    /** 대문자, 하이픈과 빈칸 뺀 값 */
    @Column(name = "piid", nullable = false, length = 100)
    private String piid;

    /** 찾은 계약. 직접 넣었으면 비어 있다 */
    @Column(name = "federal_contract_id", columnDefinition = "BINARY(16)")
    private UUID federalContractId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 10)
    private MemberContractSource source;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "potential_end_date")
    private LocalDate potentialEndDate;

    /** 공개 자료로 날짜가 바뀐 날(Asia/Seoul) (BIZ-056) */
    @Column(name = "dates_changed_on")
    private LocalDate datesChangedOn;

    /** 직접 넣은 계약의 날짜와 설명을 고친다. 공개 자료로 찾은 계약은 서비스가 막는다 */
    public void changeManual(LocalDate endDate, LocalDate potentialEndDate, String description) {
        this.endDate = endDate;
        this.potentialEndDate = potentialEndDate;
        this.description = description;
    }

    /** 공개 자료의 날짜를 따라간다. 바뀌었으면 바뀐 날을 남긴다 */
    public boolean followPublic(LocalDate endDate, LocalDate potentialEndDate, LocalDate today) {
        if (endDate == null) return false;
        boolean changed = !endDate.equals(this.endDate) || !java.util.Objects.equals(potentialEndDate, this.potentialEndDate);
        if (!changed) return false;
        this.endDate = endDate;
        this.potentialEndDate = potentialEndDate;
        this.datesChangedOn = today;
        return true;
    }
}
