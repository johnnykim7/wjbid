package com.biddingagency.domain.eligibility.entity;

import com.biddingagency.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 적격성 진단 답 (CR-137).
 *
 * 공고문 분석을 다시 돌리면 자격 순번이 다른 자격을 가리킬 수 있어, 답할 때의 자격 이름을 함께 둔다.
 * 지금 이름과 다르면 그 답은 쓰지 않는다(BIZ-033).
 */
@Entity
@Table(name = "eligibility_answers",
        uniqueConstraints = @UniqueConstraint(name = "uk_eligibility_answers",
                columnNames = {"member_id", "notice_id", "item_no"}),
        indexes = @Index(name = "idx_eligibility_answers_notice", columnList = "notice_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EligibilityAnswer extends BaseEntity {

    @Column(name = "member_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID memberId;

    @Column(name = "notice_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID noticeId;

    @Column(name = "item_no", nullable = false)
    private Integer itemNo;

    @Column(name = "item_title", nullable = false, length = 500)
    private String itemTitle;

    @Enumerated(EnumType.STRING)
    @Column(name = "answer", nullable = false, length = 20)
    private EligibilityAnswerValue answer;

    /** 있음으로 답한 자격에 붙인 증빙 서류 (CR-140, BIZ-043) */
    @Column(name = "document_id", columnDefinition = "BINARY(16)")
    private UUID documentId;

    public void change(String itemTitle, EligibilityAnswerValue answer) {
        // 붙인 서류는 그 자격의 "있음" 답에만 묶인다. 옛 답에 다시 답한 것도 다른 자격일 수 있어 푼다
        if (answer != EligibilityAnswerValue.HAVE || !matches(itemTitle)) {
            this.documentId = null;
        }
        this.itemTitle = itemTitle;
        this.answer = answer;
    }

    public void attach(UUID documentId) {
        this.documentId = documentId;
    }

    public boolean matches(String currentTitle) {
        return itemTitle.equals(currentTitle);
    }
}
