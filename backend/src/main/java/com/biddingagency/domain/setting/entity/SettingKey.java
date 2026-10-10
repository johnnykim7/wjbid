package com.biddingagency.domain.setting.entity;

/**
 * system_settings 의 설정 키 (CR-129)
 */
public final class SettingKey {

    /** 회원 월 요금(원, 부가세 별도). 비어 있으면 미정 (POL-010) */
    public static final String PRICING_MEMBER_MONTHLY_KRW = "PRICING_MEMBER_MONTHLY_KRW";

    private SettingKey() {
    }
}
