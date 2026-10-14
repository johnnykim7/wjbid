package com.biddingagency.domain.opportunity.service;

import com.biddingagency.domain.setting.entity.SettingKey;
import com.biddingagency.domain.setting.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

/**
 * PIEE 입찰서류 받기 DB 설정(POL-040, CR-160). 바꾸면 다음 받기부터 맞는다.
 * 운영 JVM 시간대와 상관없이 받기 시각은 Asia/Seoul 로 낸다.
 */
@Component
@RequiredArgsConstructor
public class PieeSettings {

    public static final String SERVICE_ZONE = "Asia/Seoul";
    public static final ZoneId ZONE = ZoneId.of(SERVICE_ZONE);
    static final String SOL_NO_SLOT = "{solNo}";
    static final long DEFAULT_MAX_ZIP_MB = 1024;
    static final long DEFAULT_TIMEOUT_SECONDS = 900;
    static final long DEFAULT_SAM_WAIT_MINUTES = 10;
    private static final long BYTES_PER_MB = 1024L * 1024L;

    private final SystemSettingService systemSettingService;
    private final Clock clock;

    public boolean enabled() {
        return systemSettingService.findValue(SettingKey.PIEE_FETCH_ENABLED)
                .map(v -> !"false".equalsIgnoreCase(v.trim()))
                .orElse(true);
    }

    /** 공고번호로 연 PIEE 공개 공고 주소. 공고번호가 비었거나 설정 주소에 {solNo} 자리가 없으면 비어 있다 */
    public Optional<String> noticeUrl(String solicitationNumber) {
        if (solicitationNumber == null || solicitationNumber.isBlank()) {
            return Optional.empty();
        }
        String solNo = URLEncoder.encode(solicitationNumber.trim(), StandardCharsets.UTF_8);
        return systemSettingService.findValue(SettingKey.PIEE_NOTICE_URL)
                .map(String::trim)
                .filter(template -> template.contains(SOL_NO_SLOT))
                .map(template -> template.replace(SOL_NO_SLOT, solNo));
    }

    public long maxZipBytes() {
        return positive(SettingKey.PIEE_MAX_ZIP_MB, DEFAULT_MAX_ZIP_MB) * BYTES_PER_MB;
    }

    public Duration timeout() {
        return Duration.ofSeconds(positive(SettingKey.PIEE_TIMEOUT_SECONDS, DEFAULT_TIMEOUT_SECONDS));
    }

    public Duration samWait() {
        return Duration.ofMinutes(positive(SettingKey.PIEE_SAM_WAIT_MINUTES, DEFAULT_SAM_WAIT_MINUTES));
    }

    public LocalDateTime now() {
        return LocalDateTime.now(clock.withZone(ZONE));
    }

    private long positive(String key, long fallback) {
        return systemSettingService.findLong(key).filter(v -> v >= 1).orElse(fallback);
    }
}
