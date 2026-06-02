package com.biddingagency.domain.opportunity.service;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * SAM.gov type 코드 → 한글 라벨 매핑 (CR-022).
 *
 * 자유 텍스트 LLM 번역이 불필요한 SAM 표준 enum. 새 코드 들어오면 한 줄만 추가.
 * 향후 관리자 편집 요구 발생 시 DB 테이블(sam_type_label)로 이전.
 */
@Component
public class SamTypeKoMapper {

    private static final Map<String, String> MAPPING = Map.of(
            "Combined Synopsis/Solicitation", "공고+입찰요청 통합",
            "Solicitation", "입찰요청",
            "Presolicitation", "사전공고",
            "Award Notice", "낙찰공지",
            "Sources Sought", "사전조사",
            "Special Notice", "특별공고",
            "Justification", "사유공고"
    );

    /** 매핑 없으면 null. 호출측은 null 시 영문 type을 그대로 보여주기. */
    public String toKo(String samType) {
        if (samType == null || samType.isBlank()) return null;
        return MAPPING.get(samType.trim());
    }
}
