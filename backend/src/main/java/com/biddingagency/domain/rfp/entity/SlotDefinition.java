package com.biddingagency.domain.rfp.entity;

import com.biddingagency.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/** 표준 슬롯 정의 (CR-013) — 시드 데이터로 관리. 코드 하드코딩 금지. */
@Entity
@Table(name = "slot_definition",
        indexes = {
                @Index(name = "uniq_slot_definition_code", columnList = "slot_code", unique = true)
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SlotDefinition extends BaseEntity {

    @Column(name = "slot_code", nullable = false, length = 40)
    private String slotCode;

    @Column(name = "label_ko", nullable = false, length = 100)
    private String labelKo;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "is_other", nullable = false)
    @Builder.Default
    private boolean isOther = false;
}
