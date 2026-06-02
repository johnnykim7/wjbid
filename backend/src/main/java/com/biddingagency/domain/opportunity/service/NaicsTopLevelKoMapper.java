package com.biddingagency.domain.opportunity.service;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * NAICS 코드 → top-level 한글 라벨 (CR-022 2차).
 *
 * NAICS 6자리 전체 매핑은 과함. 앞 2자리(sector)로 대분류만 노출.
 * 자주 등장하는 SAM 공고 sector 위주.
 * 미매핑이면 null → 화면 미노출.
 */
@Component
public class NaicsTopLevelKoMapper {

    private static final Map<String, String> SECTOR = Map.of(
            "23", "건설",
            "31", "제조업",
            "32", "제조업",
            "33", "제조업",
            "42", "도매업",
            "48", "운송업",
            "54", "전문·과학·기술 서비스",
            "56", "관리·시설지원 서비스",
            "61", "교육 서비스",
            "62", "의료·사회복지"
    );

    public String toKo(String naicsCode) {
        if (naicsCode == null || naicsCode.length() < 2) return null;
        String sector = naicsCode.substring(0, 2);
        String label = SECTOR.get(sector);
        if (label == null) return null;
        return label + " (NAICS " + naicsCode + ")";
    }
}
