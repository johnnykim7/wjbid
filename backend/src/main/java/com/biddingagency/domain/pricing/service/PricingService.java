package com.biddingagency.domain.pricing.service;

import com.biddingagency.domain.setting.entity.SettingKey;
import com.biddingagency.domain.setting.service.SystemSettingService;
import com.biddingagency.dto.PricingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 요금 조회 (CR-129, POL-010)
 *
 * 단계 이름과 포함 항목은 화면 정의에 두고, 운영 중 바뀌는 금액만 설정값에서 내준다.
 */
@Service
@RequiredArgsConstructor
public class PricingService {

    private final SystemSettingService settingService;

    public PricingResponse getPricing() {
        Long memberMonthlyPrice = settingService.findLong(SettingKey.PRICING_MEMBER_MONTHLY_KRW).orElse(null);
        return new PricingResponse(memberMonthlyPrice);
    }
}
