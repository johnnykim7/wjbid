package com.biddingagency.domain.setting.entity;

/**
 * system_settings 의 설정 키 (CR-129)
 */
public final class SettingKey {

    /** 회원 월 요금(원, 부가세 별도). 비어 있으면 미정 (POL-010) */
    public static final String PRICING_MEMBER_MONTHLY_KRW = "PRICING_MEMBER_MONTHLY_KRW";

    /** 1:1 문의 접수 알림 받는 주소(쉼표로 나눔). 비어 있으면 관리자 전원 (POL-013, CR-132) */
    public static final String INQUIRY_NOTIFY_EMAILS = "INQUIRY_NOTIFY_EMAILS";

    /** 공고문 예시 화면에 보일 공개 공고문 id(UUID). 비었거나 숨김이면 빈 안내 (POL-015, CR-135) */
    public static final String NOTICE_SAMPLE_ID = "NOTICE_SAMPLE_ID";
    /** 서류 만료 메일을 보내는 날(만료 며칠 전, 쉼표로 나눔). 비어 있거나 읽을 수 없으면 보내지 않음 (POL-016, CR-136) */
    public static final String DOC_EXPIRY_ALERT_DAYS = "DOC_EXPIRY_ALERT_DAYS";

    /** 쪽수 조건을 숫자로 읽을 때 쓰는 쪽 단위 낱말(쉼표로 나눔, 대소문자 무시). 비어 있으면 쪽수 조건은 모두 사람 확인 (POL-028, CR-141) */
    public static final String PRE_CHECK_PAGE_UNITS = "PRE_CHECK_PAGE_UNITS";

    private SettingKey() {
    }
}
