package com.biddingagency.domain.inquiry.entity;

import com.biddingagency.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 고객센터 1:1 문의 (CR-132).
 *
 * 유형·회원·답변 관리자는 연관 대신 키만 둔다. 목록과 상세를 트랜잭션 밖에서 DTO 로 옮길 때
 * 지연 로딩이 터지지 않게 하기 위해서다.
 * 답변 칸 세 개는 답변 메일이 성공했을 때 저장소의 조건부 갱신으로 한 번만 채운다(BIZ-024).
 */
@Entity
@Table(name = "inquiries",
        indexes = {
                @Index(name = "idx_inquiries_status_created", columnList = "status, created_at"),
                @Index(name = "idx_inquiries_type", columnList = "type_code"),
                @Index(name = "idx_inquiries_member", columnList = "member_id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Inquiry extends BaseEntity {

    /** 알림 발송 이력에서 이 문의를 가리키는 참조 종류 */
    public static final String REFERENCE_TYPE = "Inquiry";

    @Column(name = "type_code", nullable = false, length = 40)
    private String typeCode;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", nullable = false, length = 5000)
    private String content;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "member_id", columnDefinition = "BINARY(16)")
    private UUID memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InquiryStatus status;

    @Column(name = "answer", length = 5000)
    private String answer;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    @Column(name = "answered_by", columnDefinition = "BINARY(16)")
    private UUID answeredBy;
}
