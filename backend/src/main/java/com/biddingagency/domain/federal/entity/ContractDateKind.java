package com.biddingagency.domain.federal.entity;

/**
 * 계약 날짜 종류 (CR-142). 계약 구분의 근거 날짜와 계약 끝 알림의 기준 날짜.
 */
public enum ContractDateKind {
    /** 지금 끝날짜 */
    CURRENT_END("지금 끝날짜"),
    /** 옵션을 다 쓰면 끝나는 날짜(옵션 끝날짜) */
    POTENTIAL_END("옵션 끝날짜");

    /** 메일 문구에 쓰는 이름 */
    private final String label;

    ContractDateKind(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
