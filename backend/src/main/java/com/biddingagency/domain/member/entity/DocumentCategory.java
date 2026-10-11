package com.biddingagency.domain.member.entity;

/**
 * 회사 공통 서류함 카테고리 (CR-125, CR-147)
 *
 * fixed=true: 고정 칸. 칸마다 서류 한 건(다시 올리면 교체). 자동매칭(2단계) 대상.
 * bilingual=true: 국문·영문 칸. 언어마다 서류 한 건(BIZ-065).
 * fixed=false: OTHER(기타). 다건 허용, 자유 업로드. 자동매칭 제외.
 * CAGE 증빙, SAM 등록증은 등록 대행으로 받는 서류라 칸을 두지 않는다(CR-147).
 */
import java.util.List;
import java.util.Locale;

public enum DocumentCategory {

    BUSINESS_REGISTRATION("사업자등록증", true, false,
            List.of("사업자등록", "business registration", "business license", "사업자")),
    BUSINESS_REGISTRATION_CERTIFICATE("사업자등록증명원", true, true,
            List.of("사업자등록증명")),
    COMPANY_PROFILE("지명원·회사소개 PPT", true, true,
            List.of("지명원", "회사소개", "company profile", "capability statement", "회사 소개")),
    FINANCIAL_STATEMENT("재무제표", true, false,
            List.of("재무제표", "financial statement", "재무", "balance sheet")),
    LICENSE("인허가증", true, false,
            List.of("인허가", "허가증", "면허", "license", "permit")),
    OTHER("기타", false, false, List.of());

    private final String displayName;
    private final boolean fixed;
    private final boolean bilingual;
    /** 슬롯 제목/설명(자연어) ↔ 서류함 고정 카테고리 자동매칭용 키워드 (CR-125 2단계) */
    private final List<String> matchKeywords;

    DocumentCategory(String displayName, boolean fixed, boolean bilingual, List<String> matchKeywords) {
        this.displayName = displayName;
        this.fixed = fixed;
        this.bilingual = bilingual;
        this.matchKeywords = matchKeywords;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isFixed() {
        return fixed;
    }

    public boolean isBilingual() {
        return bilingual;
    }

    /**
     * 슬롯 제목/설명 텍스트로 고정 카테고리를 추정.
     * 슬롯엔 명시적 서류종류 enum이 없어(RequirementCategory는 유형 분류) 키워드 매칭으로 보완.
     * 맞은 낱말 중 가장 긴 낱말의 분류를 고른다. "사업자등록증명원" 이 "사업자등록" 에 먼저 걸리지 않게 하려는 것이다(BIZ-066).
     * @return 매칭된 고정 카테고리, 없으면 null
     */
    public static DocumentCategory matchByText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        DocumentCategory best = null;
        int bestLength = 0;
        for (DocumentCategory c : values()) {
            if (!c.fixed) continue;
            for (String kw : c.matchKeywords) {
                if (kw.length() > bestLength && lower.contains(kw.toLowerCase(Locale.ROOT))) {
                    best = c;
                    bestLength = kw.length();
                }
            }
        }
        return best;
    }
}
