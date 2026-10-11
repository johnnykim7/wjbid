package com.biddingagency.domain.event;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 공통 서류함 서류의 만료가 알림 시점에 닿음 (CR-136, T1-6 DocumentExpiryApproaching v1).
 * 매일 만료 확인이 서류·시점마다 한 건씩 낸다. 알림 모듈이 회원 메일로 보낸다.
 */
public class DocumentExpiryApproachingEvent extends DomainEvent {

    private final UUID memberId;
    private final String displayName;
    private final String categoryLabel;
    private final LocalDate expiresOn;
    private final int daysRemaining;

    public DocumentExpiryApproachingEvent(UUID memberDocumentId, UUID memberId, String displayName,
                                          String categoryLabel, LocalDate expiresOn, int daysRemaining) {
        super("DocumentExpiryApproaching", memberDocumentId, "MemberDocument", null);
        this.memberId = memberId;
        this.displayName = displayName;
        this.categoryLabel = categoryLabel;
        this.expiresOn = expiresOn;
        this.daysRemaining = daysRemaining;
    }

    public UUID getMemberId() { return memberId; }
    public String getDisplayName() { return displayName; }
    public String getCategoryLabel() { return categoryLabel; }
    public LocalDate getExpiresOn() { return expiresOn; }
    public int getDaysRemaining() { return daysRemaining; }
}
