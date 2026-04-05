package com.biddingagency.domain.collection.entity;

import com.biddingagency.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "keyword_groups",
        indexes = {
                @Index(name = "idx_keyword_groups_active", columnList = "active")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class KeywordGroup extends BaseEntity {

    @Column(name = "keyword", nullable = false, length = 255)
    private String keyword;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }
}
