package com.biddingagency.domain.opportunity.service;

import com.biddingagency.domain.rfp.entity.IndustryType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 공고 사업유형 자동 분류 (CR-014, BIZ-018).
 * NAICS(6자리) > PSC/classificationCode(4자리) > title 키워드 순으로 결정적 분류.
 * 매칭 실패 시 null(미분류) — UNKNOWN enum 값을 추가하지 않는다(RfpSample/PatternGuide와 enum 집합 공유).
 *
 * 확장 경계: 룰표는 현재 USFK 시설관리 용역 8개 유형 규모에 맞춘 코드 상수다.
 * 본 시스템이 수주 시스템으로 발전해 유형·룰 수가 크게 늘면, classify() 인터페이스는 그대로 두고
 * 룰 데이터만 DB 테이블(운영자 관리)로 이전한다. 호출부(수집/작성)는 영향받지 않는다.
 */
@Component
public class IndustryClassifier {

    /** NAICS 6자리 코드 → 사업유형 (연방 조달 표준 산업분류) */
    private static final Map<String, IndustryType> NAICS_RULES = Map.ofEntries(
            Map.entry("561730", IndustryType.GROUND_MAINTENANCE), // Landscaping Services
            Map.entry("561720", IndustryType.CUSTODIAL),          // Janitorial Services
            Map.entry("812320", IndustryType.LAUNDRY),            // Drycleaning & Laundry (except coin-op)
            Map.entry("812331", IndustryType.LAUNDRY),            // Linen Supply
            Map.entry("812332", IndustryType.LAUNDRY),            // Industrial Launderers
            Map.entry("238220", IndustryType.HVAC),               // Plumbing, Heating, AC Contractors
            Map.entry("811310", IndustryType.HVAC),               // Commercial Machinery (HVAC) Repair
            Map.entry("562111", IndustryType.WASTE),              // Solid Waste Collection
            Map.entry("562112", IndustryType.WASTE),              // Hazardous Waste Collection
            Map.entry("562119", IndustryType.WASTE),              // Other Waste Collection
            Map.entry("562211", IndustryType.WASTE),              // Hazardous Waste Treatment/Disposal
            Map.entry("237120", IndustryType.PIPELINE),           // Oil & Gas Pipeline Construction
            Map.entry("486910", IndustryType.PIPELINE),           // Pipeline Transportation of Refined Petroleum
            Map.entry("561612", IndustryType.SECURITY),           // Security Guards & Patrol Services
            Map.entry("561621", IndustryType.SECURITY),           // Security Systems Services
            Map.entry("531120", IndustryType.FACILITY_LEASE),     // Lessors of Nonresidential Buildings
            Map.entry("531190", IndustryType.FACILITY_LEASE)      // Lessors of Other Real Estate Property
    );

    /** PSC(Product Service Code) 4자리 → 사업유형. NAICS 미스 시 보조 (연방 표준 PSC 중 확실한 것만) */
    private static final Map<String, IndustryType> PSC_RULES = Map.ofEntries(
            Map.entry("S208", IndustryType.GROUND_MAINTENANCE),   // Housekeeping- Landscaping/Groundskeeping
            Map.entry("S201", IndustryType.CUSTODIAL),            // Housekeeping- Custodial Janitorial
            Map.entry("S209", IndustryType.LAUNDRY),              // Housekeeping- Laundry/Dry Cleaning
            Map.entry("J045", IndustryType.HVAC),                 // Maint/Repair- Plumbing, Heating, AC Equipment
            Map.entry("S205", IndustryType.WASTE),                // Housekeeping- Trash/Garbage Collection
            Map.entry("J035", IndustryType.PIPELINE),             // Maint/Repair- Pipe, Tubing, Hose, Fittings
            Map.entry("S206", IndustryType.SECURITY),             // Housekeeping- Guard
            Map.entry("R408", IndustryType.SECURITY)              // Support- Program Mgmt/Guard Services
    );

    /** title 키워드 폴백 — 순서 = 우선순위. 위에서부터 먼저 매칭되는 것 채택 */
    private static final List<Map.Entry<List<String>, IndustryType>> TITLE_KEYWORDS = List.of(
            Map.entry(List.of("ground maintenance", "grounds maintenance", "landscap", "grounds keeping", "groundskeeping"), IndustryType.GROUND_MAINTENANCE),
            Map.entry(List.of("custodial", "janitor", "cleaning", "housekeeping"), IndustryType.CUSTODIAL),
            Map.entry(List.of("laundry", "dry-cleaning", "dry cleaning", "drycleaning", "linen"), IndustryType.LAUNDRY),
            Map.entry(List.of("hvac", "heating, ventilating", "air conditioning", "air-conditioning", "ventilation"), IndustryType.HVAC),
            Map.entry(List.of("waste", "refuse", "trash", "garbage", "sludge", "disposal"), IndustryType.WASTE),
            Map.entry(List.of("pipeline", "fuel tank", "petroleum", "pol "), IndustryType.PIPELINE),
            Map.entry(List.of("security guard", "security service", "guard service", "armed guard"), IndustryType.SECURITY),
            Map.entry(List.of("lease", "rental of facilit", "facility lease", "hangar"), IndustryType.FACILITY_LEASE)
    );

    /**
     * 공고를 IndustryType으로 분류. 우선순위: NAICS > PSC > title 키워드.
     * @return 매칭된 유형, 미매칭 시 null
     */
    public IndustryType classify(String naicsCode, String classificationCode, String title) {
        IndustryType byNaics = matchExact(NAICS_RULES, naicsCode);
        if (byNaics != null) return byNaics;

        IndustryType byPsc = matchExact(PSC_RULES, classificationCode);
        if (byPsc != null) return byPsc;

        return matchTitle(title);
    }

    private IndustryType matchExact(Map<String, IndustryType> rules, String code) {
        if (code == null || code.isBlank()) return null;
        return rules.get(code.trim());
    }

    private IndustryType matchTitle(String title) {
        if (title == null || title.isBlank()) return null;
        String lower = title.toLowerCase();
        for (Map.Entry<List<String>, IndustryType> entry : TITLE_KEYWORDS) {
            for (String keyword : entry.getKey()) {
                if (lower.contains(keyword)) return entry.getValue();
            }
        }
        return null;
    }
}
