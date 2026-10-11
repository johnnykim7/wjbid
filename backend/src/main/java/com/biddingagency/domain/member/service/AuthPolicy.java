package com.biddingagency.domain.member.service;

import com.biddingagency.domain.setting.entity.SettingKey;
import com.biddingagency.domain.setting.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 인증번호(POL-037)와 SNS 표(POL-038) 정책값 (CR-146).
 * DB 설정이 비었거나 범위를 벗어나면 처음 값으로 동작한다. 사람이 행을 고쳐 넣다 틀려도 가입이 막히지 않게 하려는 것이다.
 */
@Component
@RequiredArgsConstructor
public class AuthPolicy {

    static final long DEFAULT_CODE_TTL_MINUTES = 10;
    static final long DEFAULT_RESEND_SECONDS = 60;
    static final long DEFAULT_MAX_ATTEMPTS = 5;
    static final long DEFAULT_DAILY_LIMIT = 10;
    static final long DEFAULT_VERIFIED_MINUTES = 30;
    static final long DEFAULT_SIGNUP_TICKET_MINUTES = 30;
    static final long DEFAULT_LOGIN_TICKET_SECONDS = 60;
    /** 하루를 넘는 값은 사람이 단위를 잘못 넣은 것으로 본다 */
    private static final long MAX_SECONDS_OR_COUNT = 86_400;

    private final SystemSettingService systemSettingService;

    public Duration codeTtl() {
        return Duration.ofMinutes(read(SettingKey.EMAIL_CODE_TTL_MINUTES, 1, DEFAULT_CODE_TTL_MINUTES));
    }

    public Duration resendInterval() {
        return Duration.ofSeconds(read(SettingKey.EMAIL_CODE_RESEND_SECONDS, 0, DEFAULT_RESEND_SECONDS));
    }

    public int maxAttempts() {
        return (int) read(SettingKey.EMAIL_CODE_MAX_ATTEMPTS, 1, DEFAULT_MAX_ATTEMPTS);
    }

    /** 이메일 하나에 최근 24시간 동안 보낼 수 있는 수 */
    public int dailyLimit() {
        return (int) read(SettingKey.EMAIL_CODE_DAILY_LIMIT, 1, DEFAULT_DAILY_LIMIT);
    }

    public Duration verifiedWindow() {
        return Duration.ofMinutes(read(SettingKey.EMAIL_CODE_VERIFIED_MINUTES, 1, DEFAULT_VERIFIED_MINUTES));
    }

    public Duration signupTicketTtl() {
        return Duration.ofMinutes(read(SettingKey.SNS_SIGNUP_TICKET_MINUTES, 1, DEFAULT_SIGNUP_TICKET_MINUTES));
    }

    public Duration loginTicketTtl() {
        return Duration.ofSeconds(read(SettingKey.SNS_LOGIN_TICKET_SECONDS, 1, DEFAULT_LOGIN_TICKET_SECONDS));
    }

    private long read(String key, long min, long fallback) {
        return systemSettingService.findLong(key)
                .filter(v -> v >= min && v <= MAX_SECONDS_OR_COUNT)
                .orElse(fallback);
    }
}
