package com.biddingagency.domain.opportunity.service;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * SAM.gov typeOfSetAsideDescription → 한글 라벨 (CR-022 2차).
 * 미매핑이면 null 반환 → 호출측에서 영문 그대로 노출하거나 행 숨김.
 */
@Component
public class SamSetAsideKoMapper {

    private static final Map<String, String> MAP = Map.of(
            "Total Small Business Set-Aside", "중소기업 전체 우선",
            "Total Small Business Set-Aside (FAR 19.5)", "중소기업 전체 우선 (FAR 19.5)",
            "Service-Disabled Veteran-Owned Small Business (SDVOSB) Set-Aside", "장애참전용사 중소기업 우선",
            "Women-Owned Small Business (WOSB) Set-Aside", "여성소유 중소기업 우선",
            "8(a) Set-Aside", "8(a) 사회·경제적 약자기업 우선",
            "HUBZone Set-Aside", "HUBZone(저개발지역) 우선",
            "Indian Small Business Economic Enterprise (ISBEE) Set-Aside", "원주민 중소기업 우선"
    );

    public String toKo(String samSetAside) {
        if (samSetAside == null || samSetAside.isBlank()) return null;
        return MAP.get(samSetAside.trim());
    }
}
