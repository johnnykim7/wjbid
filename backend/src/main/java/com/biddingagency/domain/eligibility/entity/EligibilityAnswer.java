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

    public void change(String itemTitle, EligibilityAnswerValue answer) {
        this.itemTitle = itemTitle;
        this.answer = answer;
    }

    public boolean matches(String currentTitle) {
        return itemTitle.equals(currentTitle);
    }
}
