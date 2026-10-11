package com.biddingagency.domain.federal.service;

import com.biddingagency.domain.setting.entity.SettingKey;
import com.biddingagency.domain.setting.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 미 연방 계약 화면의 DB 설정(POL-034, POL-036)과 한국 날짜 (CR-142). 계약 끝 알림 설정은 CR-147 에서 걷어냈다.
 * 운영 JVM 은 UTC 이므로 "오늘" 과 시각은 Asia/Seoul 로 직접 낸다.
 */
@Component
@RequiredArgsConstructor
public class FederalSettings {

    public static final String SERVICE_ZONE = "Asia/Seoul";
    public static final ZoneId ZONE = ZoneId.of(SERVICE_ZONE);
    /** AWARD_RANGE_MIN_COUNT 가 비었거나 1 보다 작을 때(POL-034) */
    static final int DEFAULT_RANGE_MIN_COUNT = 5;
    private static final int MAX_LAG_DAYS = 3650;

    private final SystemSettingService systemSettingService;
    private final Clock clock;

    public LocalDate today() {
        return LocalDate.now(clock.withZone(ZONE));
    }

    public LocalDateTime now() {
        return LocalDateTime.now(clock.withZone(ZONE));
    }

    public int rangeMinCount() {
        return systemSettingService.findLong(SettingKey.AWARD_RANGE_MIN_COUNT)
                .filter(v -> v >= 1 && v <= Integer.MAX_VALUE)
                .map(Long::intValue)
                .orElse(DEFAULT_RANGE_MIN_COUNT);
    }

    /** 늦음 일수. 비었거나 읽을 수 없으면 null, 화면은 숫자 없이 안내한다 */
    public Integer lagDays() {
        return systemSettingService.findLong(SettingKey.FEDERAL_DATA_LAG_DAYS)
                .filter(v -> v >= 0 && v <= MAX_LAG_DAYS)
                .map(Long::intValue)
                .orElse(null);
    }
}
