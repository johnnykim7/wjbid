package com.biddingagency.domain.inquiry.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 1:1 문의 유형 코드 목록 (CR-132, POL-014).
 * 화면에 보여 주기만 하고 코드가 분기하지 않으므로 Enum 이 아니라 DB 목록으로 둔다.
 */
@Entity
@Table(name = "inquiry_types")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InquiryType {

    @Id
    @Column(name = "code", length = 40)
    private String code;

    @Column(name = "label", nullable = false, length = 100)
    private String label;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "active", nullable = false)
    private Boolean active;

    public InquiryType(String code, String label, Integer sortOrder, Boolean active) {
        this.code = code;
        this.label = label;
        this.sortOrder = sortOrder;
        this.active = active;
    }
}
