package com.biddingagency.domain.rfp.entity;

import com.biddingagency.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/** 슬롯 배치 (CR-013) — 제안서 파일/섹션을 표준 7슬롯 중 하나에 배치. */
@Entity
@Table(name = "slot_assignment",
        indexes = {
                @Index(name = "idx_slot_asg_sample", columnList = "rfp_sample_id"),
                @Index(name = "idx_slot_asg_slot", columnList = "slot_definition_id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SlotAssignment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rfp_sample_id", nullable = false, columnDefinition = "BINARY(16)")
    private RfpSample rfpSample;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_definition_id", nullable = false, columnDefinition = "BINARY(16)")
    private SlotDefinition slotDefinition;

    /** 파일 단위 배치 시 (없으면 sectionText만) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sample_file_id", columnDefinition = "BINARY(16)")
    private RfpSampleFile sampleFile;

    /** slot=OTHER일 때 라벨 */
    @Column(name = "other_label", length = 100)
    private String otherLabel;

    /** 파일 일부 섹션만 배치 / 추출 실패 시 수동 입력 fallback */
    @Column(name = "section_text", columnDefinition = "longtext")
    private String sectionText;

    @Column(name = "confirmed", nullable = false)
    @Builder.Default
    private boolean confirmed = false;

    @Column(name = "auto_estimated", nullable = false)
    @Builder.Default
    private boolean autoEstimated = false;

    public void confirm() {
        this.confirmed = true;
    }

    public void updateSectionText(String sectionText) {
        this.sectionText = sectionText;
    }
}
