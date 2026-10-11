package com.biddingagency.domain.member.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 이메일 인증번호 (CR-146, BIZ-062). 번호는 해시로만 둔다.
 * 확인(verified_at)을 마친 번호는 가입 한 번에 쓰고(consumed_at) 끝난다.
 */
@Entity
@Table(name = "email_verification_codes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EmailVerificationCode {

    @Id
    @UuidGenerator
    @Column(name = "id", columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public void recordFailedAttempt() {
        this.attempts++;
    }

    public void markVerified(LocalDateTime at) {
        this.verifiedAt = at;
    }

    public void markConsumed(LocalDateTime at) {
        this.consumedAt = at;
    }
}
