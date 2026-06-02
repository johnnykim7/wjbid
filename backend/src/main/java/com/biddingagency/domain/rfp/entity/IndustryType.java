package com.biddingagency.domain.rfp.entity;

/** 사업유형 정규화 코드 (CR-013). DEFAULT = 미매칭 폴백용 기본 패턴(CR-026) */
public enum IndustryType {
    DEFAULT,
    GROUND_MAINTENANCE,
    CUSTODIAL,
    LAUNDRY,
    HVAC,
    WASTE,
    PIPELINE,
    SECURITY,
    FACILITY_LEASE
}
