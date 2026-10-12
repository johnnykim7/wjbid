package com.biddingagency.domain.opportunity.entity;

/**
 * 컴플라이언스 매트릭스 줄의 구분 (CR-159, BIZ-073). 공고 원문의 요구 동사 세기를 따른다.
 */
public enum RequirementLevel {
    /** shall, must, will */
    SHALL,
    SHOULD,
    /** 선택이거나 가점 */
    MAY;

    /** 분석 값이 비었거나 모르는 값이면 구분을 지어내지 않고 비워 둔다 */
    public static RequirementLevel fromAnalysis(Object value) {
        if (value == null) {
            return null;
        }
        for (RequirementLevel level : values()) {
            if (level.name().equalsIgnoreCase(value.toString().trim())) {
                return level;
            }
        }
        return null;
    }
}
