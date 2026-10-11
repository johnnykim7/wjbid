package com.biddingagency.domain.federal.entity;

import java.util.Arrays;
import java.util.Optional;

/**
 * 미 연방 계약 종류 (CR-142). USAspending 의 계약 종류 코드(award_type_codes)를 옮긴 것이다.
 * 계약 구분(BIZ-050)이 이 값을 보고 상위 계약 안 주문인지 가른다.
 */
public enum FederalAwardType {

    BPA_CALL("A", "BPA CALL", true),
    PURCHASE_ORDER("B", "PURCHASE ORDER", false),
    DELIVERY_ORDER("C", "DELIVERY ORDER", true),
    DEFINITIVE_CONTRACT("D", "DEFINITIVE CONTRACT", false);

    private final String code;
    private final String searchLabel;
    private final boolean order;

    FederalAwardType(String code, String searchLabel, boolean order) {
        this.code = code;
        this.searchLabel = searchLabel;
        this.order = order;
    }

    /** USAspending 검색 조건의 계약 종류 코드 */
    public String getCode() {
        return code;
    }

    /** 상위 계약 안에서 낸 주문(주문, BPA 주문)이면 참 */
    public boolean isOrder() {
        return order;
    }

    /** 검색 응답의 "Contract Award Type" 값으로 찾는다. 받는 4종이 아니면 비어 있다 */
    public static Optional<FederalAwardType> fromSearchLabel(String label) {
        if (label == null) return Optional.empty();
        String key = label.trim();
        return Arrays.stream(values()).filter(t -> t.searchLabel.equalsIgnoreCase(key)).findFirst();
    }
}
