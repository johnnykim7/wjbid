package com.biddingagency.domain.setting.service;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * 알림 시점 설정값(며칠 전, 쉼표로 나눔) 읽기 (POL-009, POL-016).
 *
 * 비었거나 하나라도 읽을 수 없거나 범위를 벗어나면 꺼짐(빈 목록)으로 본다.
 * 잘못 넣은 값으로 엉뚱한 날에 메일이 나가는 것보다 보내지 않는 편이 낫다.
 */
@Slf4j
public final class AlertDaysSetting {

    private AlertDaysSetting() {
    }

    /**
     * @param key     설정 키(로그용)
     * @param maxDays 받는 가장 긴 날
     * @return 큰 날부터 겹침 없이. 꺼져 있으면 빈 목록
     */
    public static List<Integer> parse(String key, String value, int maxDays) {
        try {
            List<Integer> days = Arrays.stream(value.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Integer::parseInt)
                    .distinct()
                    .sorted(Comparator.reverseOrder())
                    .toList();
            if (days.isEmpty() || days.stream().anyMatch(d -> d < 0 || d > maxDays)) {
                log.warn("{} 값이 비었거나 범위(0~{})를 벗어나 알림을 끕니다: {}", key, maxDays, value);
                return List.of();
            }
            return days;
        } catch (NumberFormatException e) {
            log.warn("{} 값을 읽지 못해 알림을 끕니다: {}", key, value);
            return List.of();
        }
    }
}
