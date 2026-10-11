package com.biddingagency.domain.federal.service;

import com.biddingagency.domain.setting.entity.SettingKey;
import com.biddingagency.domain.setting.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * 미 연방 계약 화면과 알림의 DB 설정(POL-034, POL-035, POL-036)과 한국 날짜 (CR-142).
 * 운영 JVM 은 UTC 이므로 "오늘" 과 시각은 Asia/Seoul 로 직접 낸다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FederalSettings {

    public static final String SERVICE_ZONE = "Asia/Seoul";
    public static final ZoneId ZONE = ZoneId.of(SERVICE_ZONE);
    /** AWARD_RANGE_MIN_COUNT 가 비었거나 1 보다 작을 때(POL-034) */
    static final int DEFAULT_RANGE_MIN_COUNT = 5;
    private static final int MAX_ALERT_DAYS = 3650;

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
                .filter(v -> v >= 0 && v <= MAX_ALERT_DAYS)
                .map(Long::intValue)
                .orElse(null);
    }

    /** 한 번짜리 공사 꼬리표 업종 앞자리. 비었으면 꼬리표 없음 */
    public List<String> oneTimeNaicsPrefixes() {
        return systemSettingService.findValue(SettingKey.RECOMPETE_ONE_TIME_NAICS_PREFIXES)
                .map(v -> Arrays.stream(v.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList())
                .orElse(List.of());
    }

    /**
     * 계약 끝 알림 시점(큰 날부터). 값이 비었거나 하나라도 읽을 수 없으면 꺼짐(빈 목록).
     * 잘못 넣은 값으로 엉뚱한 날에 메일이 나가는 것보다 보내지 않는 편이 낫다.
     */
    public List<Integer> alertDays() {
        return systemSettingService.findValue(SettingKey.CONTRACT_END_ALERT_DAYS).map(value -> {
            try {
                List<Integer> days = Arrays.stream(value.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(Integer::parseInt)
                        .distinct()
                        .sorted(Comparator.reverseOrder())
                        .toList();
                if (days.isEmpty() || days.stream().anyMatch(d -> d < 0 || d > MAX_ALERT_DAYS)) {
                    log.warn("계약 끝 알림 시점이 범위를 벗어나 알림을 끕니다: {}", value);
                    return List.<Integer>of();
                }
                return days;
            } catch (NumberFormatException e) {
                log.warn("계약 끝 알림 시점을 읽지 못해 알림을 끕니다: {}", value);
                return List.<Integer>of();
            }
        }).orElse(List.of());
    }
}
