package com.biddingagency.domain.compliance.entity;

/**
 * 요구 항목을 맡는 쪽 (CR-141, BIZ-045). 공고 분석의 제출물 fulfillmentParty 값과 같다.
 */
public enum FulfillmentParty {
    CLIENT_UPLOAD,
    PLATFORM_GENERATED,
    SYSTEM_FORM;

    /**
     * 분석 값이 비었거나 모르는 값이면 고객 올림으로 본다.
     * 고객 화면에 보여 고객이 올리거나 검토를 신청할 수 있게 두는 편이 숨기는 것보다 낫다.
     */
    public static FulfillmentParty fromAnalysis(String value) {
        if (value == null) {
            return CLIENT_UPLOAD;
        }
        for (FulfillmentParty party : values()) {
            if (party.name().equalsIgnoreCase(value.trim())) {
                return party;
            }
        }
        return CLIENT_UPLOAD;
    }
}
