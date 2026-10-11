package com.biddingagency.domain.member.service;

import com.biddingagency.domain.setting.service.SystemSettingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

/**
 * TC-AUTH-017: 정책값이 비었을 때 처음 값 (CR-146, POL-037, POL-038)
 */
@ExtendWith(MockitoExtension.class)
class AuthPolicyTest {

    @Mock
    private SystemSettingService systemSettingService;

    @InjectMocks
    private AuthPolicy policy;

    @Test
    @DisplayName("설정이빔_정책값_처음값으로")
    void 설정빔_처음값() {
        given(systemSettingService.findLong(anyString())).willReturn(Optional.empty());

        assertThat(new Object[]{policy.codeTtl(), policy.resendInterval(), policy.maxAttempts(), policy.dailyLimit(),
                policy.verifiedWindow(), policy.signupTicketTtl(), policy.loginTicketTtl()})
                .containsExactly(Duration.ofMinutes(10), Duration.ofSeconds(60), 5, 10,
                        Duration.ofMinutes(30), Duration.ofMinutes(30), Duration.ofSeconds(60));
    }

    @Test
    @DisplayName("설정이범위밖_정책값_처음값으로")
    void 범위밖_처음값() {
        given(systemSettingService.findLong(anyString())).willReturn(Optional.of(-1L));

        assertThat(policy.maxAttempts()).isEqualTo(5);
    }
}
