package com.biddingagency.domain.member.service;

import com.biddingagency.domain.member.AuthError;
import com.biddingagency.domain.member.AuthFlowException;
import com.biddingagency.domain.member.EmailAlreadyExistsException;
import com.biddingagency.domain.member.entity.EmailVerificationCode;
import com.biddingagency.domain.member.repository.EmailVerificationCodeRepository;
import com.biddingagency.domain.member.repository.MemberRepository;
import com.biddingagency.integration.notification.BpNotificationClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;

/**
 * 이메일 인증번호 보내기와 확인 (CR-146, BID-AUTH-005, BIZ-062).
 * 메일은 알림 서버 틀 BID_EMAIL_CODE 로 요청 안에서 보낸다. 보내지 못하면 번호도 남기지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailCodeService {

    static final String TEMPLATE_CODE = "BID_EMAIL_CODE";
    private static final int CODE_BOUND = 1_000_000;
    // 하루 한도의 날짜는 서울 날짜다(POL-037). 저장 시각은 시계 시간대라 그날 0시를 그 시간대로 옮겨 센다
    private static final ZoneId DAILY_LIMIT_ZONE = ZoneId.of("Asia/Seoul");

    private final EmailVerificationCodeRepository codeRepository;
    private final MemberRepository memberRepository;
    private final BpNotificationClient notificationClient;
    private final AuthPolicy policy;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public record Sent(long expiresInSeconds, long resendAfterSeconds) {
    }

    @Transactional
    public Sent send(String rawEmail) {
        String email = normalize(rawEmail);
        if (memberRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException();
        }
        LocalDateTime now = LocalDateTime.now(clock);
        Duration resend = policy.resendInterval();
        codeRepository.findFirstByEmailOrderByCreatedAtDesc(email).ifPresent(last -> {
            LocalDateTime allowedAt = last.getCreatedAt().plus(resend);
            if (now.isBefore(allowedAt)) {
                long wait = Duration.between(now, allowedAt).toMillis();
                throw new AuthFlowException(AuthError.EMAIL_CODE_TOO_SOON,
                        Map.of("retryAfterSeconds", Math.max(1, (wait + 999) / 1000)));
            }
        });
        if (codeRepository.countByEmailAndCreatedAtGreaterThanEqual(email, startOfToday()) >= policy.dailyLimit()) {
            throw new AuthFlowException(AuthError.EMAIL_CODE_DAILY_LIMIT);
        }

        String code = String.format("%06d", random.nextInt(CODE_BOUND));
        Duration ttl = policy.codeTtl();
        codeRepository.save(EmailVerificationCode.builder()
                .email(email)
                .codeHash(hash(email, code))
                .expiresAt(now.plus(ttl))
                .createdAt(now)
                .build());

        BpNotificationClient.SendResult result = notificationClient.sendEmail(email, TEMPLATE_CODE,
                Map.of("code", code, "validMinutes", ttl.toMinutes()));
        if (!result.success()) {
            throw new AuthFlowException(AuthError.EMAIL_SEND_FAILED);
        }
        return new Sent(ttl.toSeconds(), resend.toSeconds());
    }

    /** 틀린 횟수는 오류로 끝나도 남아야 하므로 이 트랜잭션은 AuthFlowException 에 되돌리지 않는다 */
    @Transactional(noRollbackFor = AuthFlowException.class)
    public void verify(String rawEmail, String code) {
        String email = normalize(rawEmail);
        EmailVerificationCode latest = codeRepository.findFirstByEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new AuthFlowException(AuthError.EMAIL_CODE_EXPIRED));
        if (latest.getConsumedAt() != null) {
            throw new AuthFlowException(AuthError.EMAIL_CODE_EXPIRED);
        }
        if (latest.getVerifiedAt() != null) {
            return;
        }
        int max = policy.maxAttempts();
        if (latest.getAttempts() >= max) {
            throw new AuthFlowException(AuthError.EMAIL_CODE_LOCKED);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (now.isAfter(latest.getExpiresAt())) {
            throw new AuthFlowException(AuthError.EMAIL_CODE_EXPIRED);
        }
        if (code == null || !matches(latest.getCodeHash(), hash(email, code.trim()))) {
            latest.recordFailedAttempt();
            int remaining = max - latest.getAttempts();
            if (remaining <= 0) {
                throw new AuthFlowException(AuthError.EMAIL_CODE_LOCKED);
            }
            throw new AuthFlowException(AuthError.EMAIL_CODE_MISMATCH, Map.of("remainingAttempts", remaining));
        }
        latest.markVerified(now);
    }

    /**
     * 가입 때 부른다. 확인을 마친 지 정해진 시간 안의 번호를 가입 한 번에 쓰고 끝낸다.
     * 없으면 EMAIL_NOT_VERIFIED 로 가입을 막는다.
     */
    @Transactional
    public void consumeVerified(String rawEmail) {
        String email = normalize(rawEmail);
        LocalDateTime now = LocalDateTime.now(clock);
        Duration window = policy.verifiedWindow();
        EmailVerificationCode verified = codeRepository
                .findFirstByEmailAndVerifiedAtIsNotNullAndConsumedAtIsNullOrderByVerifiedAtDesc(email)
                .filter(c -> !now.isAfter(c.getVerifiedAt().plus(window)))
                .orElseThrow(() -> new AuthFlowException(AuthError.EMAIL_NOT_VERIFIED));
        verified.markConsumed(now);
    }

    private LocalDateTime startOfToday() {
        LocalDate today = LocalDate.now(clock.withZone(DAILY_LIMIT_ZONE));
        return LocalDateTime.ofInstant(today.atStartOfDay(DAILY_LIMIT_ZONE).toInstant(), clock.getZone());
    }

    static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    static String hash(String email, String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest((email + ":" + code).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean matches(String expectedHex, String actualHex) {
        return MessageDigest.isEqual(expectedHex.getBytes(StandardCharsets.UTF_8),
                actualHex.getBytes(StandardCharsets.UTF_8));
    }
}
