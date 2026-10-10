package com.biddingagency.domain.pricing.service;

import com.biddingagency.domain.setting.entity.SettingKey;
import com.biddingagency.domain.setting.entity.SystemSetting;
import com.biddingagency.domain.setting.repository.SystemSettingRepository;
import com.biddingagency.domain.setting.service.SystemSettingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * TC-PRICE-001 ~ TC-PRICE-004: PricingService 테스트 (CR-129)
 */
@ExtendWith(MockitoExtension.class)
class PricingServiceTest {

    @Mock
    private SystemSettingRepository repository;

    private PricingService pricingService;

    @BeforeEach
    void setUp() {
        pricingService = new PricingService(new SystemSettingService(repository));
    }

    private void givenMemberPrice(String value) {
        given(repository.findById(SettingKey.PRICING_MEMBER_MONTHLY_KRW)).willReturn(Optional.of(
                SystemSetting.builder().settingKey(SettingKey.PRICING_MEMBER_MONTHLY_KRW).settingValue(value).build()));
    }

    // TC-PRICE-001
    @Test
    @DisplayName("회원금액설정있음_요금조회_금액반환됨")
    void 회원금액설정있음_요금조회_금액반환됨() {
        givenMemberPrice("50000");

        assertThat(pricingService.getPricing().memberMonthlyPrice()).isEqualTo(50000L);
    }

    // TC-PRICE-002
    @Test
    @DisplayName("회원금액비어있음_요금조회_null반환됨")
    void 회원금액비어있음_요금조회_null반환됨() {
        givenMemberPrice(" ");

        assertThat(pricingService.getPricing().memberMonthlyPrice()).isNull();
    }

    // TC-PRICE-003
    @Test
    @DisplayName("설정행없음_요금조회_null반환됨")
    void 설정행없음_요금조회_null반환됨() {
        given(repository.findById(SettingKey.PRICING_MEMBER_MONTHLY_KRW)).willReturn(Optional.empty());

        assertThat(pricingService.getPricing().memberMonthlyPrice()).isNull();
    }

    // TC-PRICE-004
    @Test
    @DisplayName("숫자아닌값_요금조회_null반환됨")
    void 숫자아닌값_요금조회_null반환됨() {
        givenMemberPrice("5만원");

        assertThat(pricingService.getPricing().memberMonthlyPrice()).isNull();
    }
}
