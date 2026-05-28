package com.biddingagency.domain.rfp.entity;

import com.biddingagency.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/** 성공 제안서 원본 파일 (CR-013) — 로컬 디스크 보관. 텍스트 추출/파싱은 Aimbase가 담당. */
@Entity
@Table(name = "rfp_sample_file",
        indexes = {
                @Index(name = "idx_rfp_file_sample", columnList = "rfp_sample_id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RfpSampleFile extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rfp_sample_id", nullable = false, columnDefinition = "BINARY(16)")
    private RfpSample rfpSample;

    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "storage_url", columnDefinition = "TEXT", nullable = false)
    private String storageUrl;

    /** PWS 공고문 파일 여부 (A경로용) */
    @Column(name = "is_pws", nullable = false)
    @Builder.Default
    private boolean isPws = false;
}
