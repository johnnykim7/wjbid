package com.biddingagency.domain.sharedfile.service;

import com.biddingagency.domain.setting.entity.SettingKey;
import com.biddingagency.domain.setting.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** 자료실 정책값(POL-042). 비었거나 범위를 벗어나면 처음 값을 쓴다. */
@Component
@RequiredArgsConstructor
public class SharedFilePolicy {

    static final long DEFAULT_MAX_UPLOAD_MB = 50;
    static final long DEFAULT_MAX_ATTEMPTS = 5;
    static final long DEFAULT_LOCK_MINUTES = 30;
    static final String DEFAULT_BLOCKED_EXTENSIONS =
            "exe,bat,cmd,com,scr,msi,ps1,vbs,js,jar,sh,html,htm,xhtml,svg,php,jsp";

    private final SystemSettingService systemSettingService;

    public long maxUploadMb() {
        return read(SettingKey.SHARED_FILE_MAX_UPLOAD_MB, DEFAULT_MAX_UPLOAD_MB);
    }

    public int maxAttempts() {
        return (int) read(SettingKey.SHARED_FILE_MAX_ATTEMPTS, DEFAULT_MAX_ATTEMPTS);
    }

    public Duration lockDuration() {
        return Duration.ofMinutes(read(SettingKey.SHARED_FILE_LOCK_MINUTES, DEFAULT_LOCK_MINUTES));
    }

    public List<String> blockedExtensions() {
        String value = systemSettingService.findValue(SettingKey.SHARED_FILE_BLOCKED_EXTENSIONS)
                .filter(v -> !v.isBlank())
                .orElse(DEFAULT_BLOCKED_EXTENSIONS);
        return Arrays.stream(value.split(","))
                .map(v -> v.trim().toLowerCase(Locale.ROOT))
                .filter(v -> !v.isEmpty())
                .toList();
    }

    private long read(String key, long fallback) {
        return systemSettingService.findLong(key).filter(v -> v >= 1).orElse(fallback);
    }
}
