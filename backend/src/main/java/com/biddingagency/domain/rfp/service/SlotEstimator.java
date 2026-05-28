package com.biddingagency.domain.rfp.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 파일명 기반 슬롯 자동추정 (CR-013). 단순 키워드 휴리스틱 — 관리자 confirmed 토글로 보정 전제.
 * AI 자동분류는 1차 범위 밖(YAGNI).
 */
@Component
public class SlotEstimator {

    /** slotCode → 매칭 키워드(소문자) 목록. 위에서부터 첫 매칭 슬롯으로 추정. */
    private static final List<Map.Entry<String, List<String>>> RULES = List.of(
            Map.entry("PRICE", List.of("price", "pricing", "cost", "가격", "단가", "견적")),
            Map.entry("PAST_PERFORMANCE", List.of("past performance", "performance", "실적", "수행실적")),
            Map.entry("PRIOR_EXPERIENCE", List.of("prior experience", "experience", "경험", "수행경험", "subfactor2", "sub-factor 2")),
            Map.entry("KEY_PERSONNEL", List.of("key personnel", "personnel", "cm", "qcm", "핵심인력", "관리자")),
            Map.entry("STAFFING_PLAN", List.of("staffing", "management plan", "인력배치", "인력 배치", "staff")),
            Map.entry("EQUIPMENT_PLAN", List.of("equipment", "tools", "장비")),
            Map.entry("BIZ_QUALIFICATION", List.of("business license", "registration", "qualification", "사업자", "등록증", "면허", "subfactor1", "sub-factor 1"))
    );

    /** 매칭되는 slotCode를 반환. 없으면 null(관리자가 직접 배치). */
    public String estimate(String fileName) {
        if (fileName == null) return null;
        String lower = fileName.toLowerCase();
        for (Map.Entry<String, List<String>> rule : RULES) {
            for (String kw : rule.getValue()) {
                if (lower.contains(kw)) {
                    return rule.getKey();
                }
            }
        }
        return null;
    }
}
