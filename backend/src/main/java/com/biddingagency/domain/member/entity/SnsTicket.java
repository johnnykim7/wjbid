package com.biddingagency.domain.member.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * SNS 에서 돌아온 뒤 화면 주소에 실어 보내는 한 번 쓰는 표 (CR-146).
 * 토큰을 주소에 바로 싣지 않으려고 둔다. 표 값은 해시로만 두고, 쓰면 used_at 을 채워 다시 받지 않는다.
 */
@Entity
@Table(name = "sns_tickets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SnsTicket {

    @Id
    @UuidGenerator
    @Column(name = "id", columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 10)
    private SnsTicketKind kind;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private SnsProvider provider;

    @Column(name = "provider_user_id", nullable = false, length = 100)
    private String providerUserId;

    @Column(name = "provider_email")
    private String providerEmail;

    @Column(name = "email_verified_by_provider", nullable = false)
    private boolean emailVerifiedByProvider;

    /** LOGIN 일 때 이어진 회원 */
    @Column(name = "member_id", columnDefinition = "BINARY(16)")
    private UUID memberId;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public boolean isUsable(SnsTicketKind expected, LocalDateTime now) {
        return kind == expected && usedAt == null && !now.isAfter(expiresAt);
    }

    public void markUsed(LocalDateTime at) {
        this.usedAt = at;
    }
}
