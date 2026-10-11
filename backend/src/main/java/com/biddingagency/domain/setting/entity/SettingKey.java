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

    /** 낙찰가 범위를 보이는 최소 계약 수. 비었거나 1 보다 작으면 5 (POL-034, CR-142) */
    public static final String AWARD_RANGE_MIN_COUNT = "AWARD_RANGE_MIN_COUNT";
    /** 미 연방 계약 자료가 늦게 올라오는 날 수. 화면 안내에만 쓴다 (POL-036, CR-142) */
    public static final String FEDERAL_DATA_LAG_DAYS = "FEDERAL_DATA_LAG_DAYS";

    /** 고객 화면 사진으로 받는 형식(쉼표로 나눔). 비어 있으면 image/jpeg,image/png (POL-039, CR-153) */
    public static final String SITE_PHOTO_ALLOWED_TYPES = "SITE_PHOTO_ALLOWED_TYPES";
    /** 고객 화면 사진 한 장 무게 상한(MB). 비었거나 1 보다 작으면 20 (POL-039, CR-153) */
    public static final String SITE_PHOTO_MAX_UPLOAD_MB = "SITE_PHOTO_MAX_UPLOAD_MB";
    /** 고객 화면 사진 원본 화소 수 상한(백만 화소). 비었거나 1 보다 작으면 50 (POL-039, CR-153) */
    public static final String SITE_PHOTO_MAX_MEGAPIXELS = "SITE_PHOTO_MAX_MEGAPIXELS";
    /** 고객 화면 사진 가로 최소. 비었거나 1 보다 작으면 1000 (POL-039, CR-153) */
    public static final String SITE_PHOTO_MIN_WIDTH = "SITE_PHOTO_MIN_WIDTH";
    /** 고객 화면 사진 저장 가로. 비었거나 1 보다 작으면 1600 (POL-039, CR-153) */
    public static final String SITE_PHOTO_SAVE_WIDTH = "SITE_PHOTO_SAVE_WIDTH";
    /** 고객 화면 사진 저장 무게 목표(KB). 비었거나 1 보다 작으면 400 (POL-039, CR-153) */
    public static final String SITE_PHOTO_SAVE_MAX_KB = "SITE_PHOTO_SAVE_MAX_KB";

    private SettingKey() {
    }
}
