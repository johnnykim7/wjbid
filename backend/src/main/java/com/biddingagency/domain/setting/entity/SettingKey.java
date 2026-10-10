package com.biddingagency.domain.setting.entity;

/**
 * system_settings 의 설정 키 (CR-129)
 */
public final class SettingKey {

    /** 회원 월 요금(원, 부가세 별도). 비어 있으면 미정 (POL-010) */
    public static final String PRICING_MEMBER_MONTHLY_KRW = "PRICING_MEMBER_MONTHLY_KRW";

    /** 1:1 문의 접수 알림 받는 주소(쉼표로 나눔). 비어 있으면 관리자 전원 (POL-013, CR-132) */
    public static final String INQUIRY_NOTIFY_EMAILS = "INQUIRY_NOTIFY_EMAILS";

    private SettingKey() {
    }
}
