package com.biddingagency.domain.member.entity;

/**
 * 회사 공통 서류함 카테고리 (CR-125)
 *
 * fixed=true: 고정 슬롯. member당 최대 1건(재업로드=교체). 자동매칭(2단계) 대상.
 * fixed=false: OTHER(기타). 다건 허용, 자유 업로드. 자동매칭 제외.
 */
import java.util.List;
import java.util.Locale;

public enum DocumentCategory {

    BUSINESS_REGISTRATION("사업자등록증", true,
            List.of("사업자등록", "business registration", "business license", "사업자")),
    CAGE("CAGE 증빙", true,
            List.of("cage")),
    COMPANY_PROFILE("회사소개서", true,
            List.of("회사소개", "company profile", "capability statement", "회사 소개")),
    FINANCIAL_STATEMENT("재무제표", true,
            List.of("재무제표", "financial statement", "재무", "balance sheet")),
    SAM_REGISTRATION("SAM 등록증", true,
            List.of("sam 등록", "sam.gov", "sam registration", "system for award management")),
    PAST_PERFORMANCE("과거 수행실적", true,
            List.of("수행실적", "past performance", "실적", "performance record")),
    OTHER("기타", false, List.of());

    private final String displayName;
    private final boolean fixed;
    /** 슬롯 제목/설명(자연어) ↔ 서류함 고정 카테고리 자동매칭용 키워드 (CR-125 2단계) */
    private final List<String> matchKeywords;

    DocumentCategory(String displayName, boolean fixed, List<String> matchKeywords) {
        this.displayName = displayName;
        this.fixed = fixed;
        this.matchKeywords = matchKeywords;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isFixed() {
        return fixed;
    }

    /**
     * 슬롯 제목/설명 텍스트로 고정 카테고리를 추정.
     * 슬롯엔 명시적 서류종류 enum이 없어(RequirementCategory는 유형 분류) 키워드 매칭으로 보완.
     * @return 매칭된 고정 카테고리, 없으면 null
     */
    public static DocumentCategory matchByText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        for (DocumentCategory c : values()) {
            if (!c.fixed) continue;
            for (String kw : c.matchKeywords) {
                if (lower.contains(kw.toLowerCase(Locale.ROOT))) {
                    return c;
                }
            }
        }
        return null;
    }
}
