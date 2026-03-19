package com.biddingagency.domain.bookmark.entity;

import com.biddingagency.common.BaseEntity;
import com.biddingagency.domain.member.entity.Member;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "bookmarks",
        uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "opportunity_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Bookmark extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opportunity_id", nullable = false)
    private Opportunity opportunity;
}
