package com.biddingagency.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 1:1 문의 접수 (CR-132, T1-6 InquiryReceived v1).
 * 접수가 커밋된 뒤 관리자 알림 리스너가 받는다. 비로그인 문의면 memberId 가 비어 있다.
 */
public class InquiryReceivedEvent extends DomainEvent {
    private final String typeLabel;
    private final String title;
    private final String content;
    private final String name;
    private final String email;
    private final UUID memberId;
    private final LocalDateTime receivedAt;

    public InquiryReceivedEvent(UUID inquiryId, String typeLabel, String title, String content,
                                String name, String email, UUID memberId, LocalDateTime receivedAt) {
        super("InquiryReceived", inquiryId, "Inquiry", memberId);
        this.typeLabel = typeLabel;
        this.title = title;
        this.content = content;
        this.name = name;
        this.email = email;
        this.memberId = memberId;
        this.receivedAt = receivedAt;
    }

    public String getTypeLabel() { return typeLabel; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public UUID getMemberId() { return memberId; }
    public LocalDateTime getReceivedAt() { return receivedAt; }
}
