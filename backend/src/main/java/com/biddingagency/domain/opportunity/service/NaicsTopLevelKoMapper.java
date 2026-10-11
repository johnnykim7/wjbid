package com.biddingagency.domain.opportunity.service;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * NAICS 코드 → 대분류(sector) 한글 이름 (CR-022 2차, CR-135 에서 공식 대분류 20개 전부로 넓힘).
 *
 * NAICS 6자리 전체 매핑은 과함. 앞 2자리로 대분류만 노출한다.
 * 31~33, 44~45, 48~49 는 공식 정의상 한 대분류라 같은 키로 묶는다.
 */
@Component
public class NaicsTopLevelKoMapper {

    /** 업종 코드가 없는 공고의 대분류 키 (CR-135 업종별 발주의 "업종 표시 없음") */
    public static final String NONE = "NONE";

    private static final Map<String, String> RANGE_KEY = Map.of(
            "31", "31-33", "32", "31-33", "33", "31-33",
            "44", "44-45", "45", "44-45",
            "48", "48-49", "49", "48-49"
    );

    private static final Map<String, String> SECTOR = Map.ofEntries(
            Map.entry("11", "농림어업"),
            Map.entry("21", "광업"),
            Map.entry("22", "전기·가스·수도"),
            Map.entry("23", "건설"),
            Map.entry("31-33", "제조업"),
            Map.entry("42", "도매업"),
            Map.entry("44-45", "소매업"),
            Map.entry("48-49", "운송·창고업"),
            Map.entry("51", "정보 서비스"),
            Map.entry("52", "금융·보험"),
            Map.entry("53", "부동산·임대"),
            Map.entry("54", "전문·과학·기술 서비스"),
            Map.entry("55", "기업 관리"),
            Map.entry("56", "사업 지원·폐기물 처리"),
            Map.entry("61", "교육 서비스"),
            Map.entry("62", "의료·사회복지"),
            Map.entry("71", "예술·오락·여가"),
            Map.entry("72", "숙박·음식점"),
            Map.entry("81", "기타 서비스"),
            Map.entry("92", "공공 행정")
    );

    public String toKo(String naicsCode) {
        if (naicsCode == null || naicsCode.trim().length() < 2) return null;
        String label = sectorKo(sectorOf(naicsCode));
        if (label == null) return null;
        return label + " (NAICS " + naicsCode.trim() + ")";
    }

    /** 업종 코드의 대분류 키(예 23, 31-33). 코드가 없거나 두 자리가 안 되면 NONE */
    public String sectorOf(String naicsCode) {
        if (naicsCode == null) return NONE;
        String code = naicsCode.trim();
        if (code.length() < 2) return NONE;
        String two = code.substring(0, 2);
        return RANGE_KEY.getOrDefault(two, two);
    }

    /** 대분류 키의 한글 이름. 공식 대분류가 아니면 null */
    public String sectorKo(String sectorKey) {
        if (sectorKey == null) return null;
        return SECTOR.get(sectorKey);
    }
}
