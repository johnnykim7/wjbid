package com.biddingagency.domain.setting.service;

import com.biddingagency.domain.setting.entity.SystemSetting;
import com.biddingagency.domain.setting.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 운영 설정값 조회 (CR-129)
 *
 * 행이 없거나 값이 비었거나 형식이 맞지 않으면 모두 "미정" 으로 본다.
 * 값은 사람이 행을 직접 고쳐 넣으므로 잘못 넣은 값으로 화면이 깨지지 않게 하기 위해서다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemSettingService {

    private final SystemSettingRepository repository;

    @Transactional(readOnly = true)
    public Optional<String> findValue(String key) {
        return repository.findById(key)
                .map(SystemSetting::getSettingValue)
                .map(String::trim)
                .filter(v -> !v.isEmpty());
    }

    @Transactional(readOnly = true)
    public Optional<Long> findLong(String key) {
        return findValue(key).flatMap(v -> {
            try {
                return Optional.of(Long.parseLong(v));
            } catch (NumberFormatException e) {
                log.warn("설정값을 정수로 읽지 못해 미정으로 봅니다: key={}, value={}", key, v);
                return Optional.empty();
            }
        });
    }
}
