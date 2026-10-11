package com.biddingagency.domain.opportunity;

import java.util.Map;

/**
 * 모은 공고를 화면에서 나누는 묶음 유형 (CR-135, T3-2 C-2).
 *
 * <p>SAM 공고 유형은 일곱 가지가 넘고 늘 수 있어, 메뉴 속 화면은 넷으로만 나눈다.
 * 여기 없는 유형은 모두 OTHER 로 센다.
 */
public enum MarketGroup {
    PRE,
    BID,
    AWARD,
    OTHER;

    private static final Map<String, MarketGroup> BY_SAM_TYPE = Map.of(
            "Presolicitation", PRE,
            "Sources Sought", PRE,
            "Solicitation", BID,
            "Combined Synopsis/Solicitation", BID,
            "Award Notice", AWARD
    );

    public static MarketGroup of(String samType) {
        if (samType == null) return OTHER;
        return BY_SAM_TYPE.getOrDefault(samType.trim(), OTHER);
    }
}
