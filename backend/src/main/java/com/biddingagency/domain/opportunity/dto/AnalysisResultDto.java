package com.biddingagency.domain.opportunity.dto;

import com.biddingagency.domain.notice.entity.Notice;
import com.biddingagency.domain.notice.entity.NoticeGenerationStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AnalysisResultDto(
        String analysisStatus,
        String analyzedAt,
        SummaryDto summary,
        RequiredDocumentsDto requiredDocuments,
        DocumentFormatsDto documentFormats,
        // CR-021: TipTap JSON 본문 — FE 렌더러가 노드 트리 그대로 해석
        Map<String, Object> contentJson
) {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** CR-016: 한글화 결과는 Notice가 보유 */
    public static AnalysisResultDto fromNotice(Notice notice) {
        if (notice == null) return null;

        String status = notice.getGenerationStatus() != null ? notice.getGenerationStatus().name() : null;
        String analyzedAt = notice.getAnalyzedAt() != null ? notice.getAnalyzedAt().format(FMT) : null;

        if (notice.getGenerationStatus() != NoticeGenerationStatus.COMPLETED) {
            return new AnalysisResultDto(status, analyzedAt, null, null, null, null);
        }

        return new AnalysisResultDto(
                status,
                analyzedAt,
                SummaryDto.from(notice.getSummaryJson()),
                RequiredDocumentsDto.from(notice.getRequiredDocumentsJson()),
                DocumentFormatsDto.from(notice.getDocumentFormatsJson()),
                notice.getContentJson()
        );
    }

    // ── Summary ──────────────────────────────────────────

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SummaryDto(
            String overview,
            String scope,
            String eligibility,
            String evaluationCriteria,
            List<KeyDateDto> keyDates,
            String budgetInfo,
            List<String> specialNotes
    ) {
        @SuppressWarnings("unchecked")
        public static SummaryDto from(Map<String, Object> map) {
            if (map == null || map.isEmpty()) return null;
            List<KeyDateDto> dates = null;
            Object raw = map.get("keyDates");
            if (raw instanceof List<?> list) {
                dates = list.stream()
                        .filter(e -> e instanceof Map)
                        .map(e -> KeyDateDto.from((Map<String, Object>) e))
                        .toList();
            }
            List<String> notes = null;
            Object rawNotes = map.get("specialNotes");
            if (rawNotes instanceof List<?> list) {
                notes = list.stream().map(Object::toString).toList();
            }
            return new SummaryDto(
                    str(map, "overview"),
                    str(map, "scope"),
                    str(map, "eligibility"),
                    str(map, "evaluationCriteria"),
                    dates,
                    str(map, "budgetInfo"),
                    notes
            );
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record KeyDateDto(String label, String date, String note) {
        public static KeyDateDto from(Map<String, Object> m) {
            return new KeyDateDto(str(m, "label"), str(m, "date"), str(m, "note"));
        }
    }

    // ── Required Documents ───────────────────────────────

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record RequiredDocumentsDto(List<RequiredDocumentItem> documents, List<FactorDto> factors,
                                       List<EligibilityDto> eligibility) {
        @SuppressWarnings("unchecked")
        public static RequiredDocumentsDto from(Map<String, Object> map) {
            if (map == null || map.isEmpty()) return null;
            // 평면 documents[] (하위호환)
            Object raw = map.get("documents");
            List<RequiredDocumentItem> items = (raw instanceof List<?> list)
                    ? list.stream().filter(e -> e instanceof Map)
                        .map(e -> RequiredDocumentItem.from((Map<String, Object>) e)).toList()
                    : Collections.emptyList();
            // CR-033: FACTOR>Subfactor 트리
            Object rawF = map.get("factors");
            List<FactorDto> factors = (rawF instanceof List<?> fl)
                    ? fl.stream().filter(e -> e instanceof Map)
                        .map(e -> FactorDto.from((Map<String, Object>) e)).toList()
                    : null;
            // CR-033: 자격요건
            Object rawE = map.get("eligibility");
            List<EligibilityDto> eligibility = (rawE instanceof List<?> el)
                    ? el.stream().filter(e -> e instanceof Map)
                        .map(e -> EligibilityDto.from((Map<String, Object>) e)).toList()
                    : null;
            return new RequiredDocumentsDto(items, factors, eligibility);
        }
    }

    // CR-033: 자격요건 (서류와 같은 근거에서 추출. 자격=제출 증빙의 동전 양면)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record EligibilityDto(
            String title, String description, Boolean mandatory,
            String evidenceBy, Boolean isGate, String sourceRef
    ) {
        public static EligibilityDto from(Map<String, Object> m) {
            return new EligibilityDto(
                    str(m, "title"), str(m, "description"), bool(m, "mandatory"),
                    str(m, "evidenceBy"), bool(m, "isGate"), str(m, "sourceRef")
            );
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record RequiredDocumentItem(
            String name, String description, Boolean mandatory,
            String format, String pageLimit, String notes
    ) {
        public static RequiredDocumentItem from(Map<String, Object> m) {
            return new RequiredDocumentItem(
                    str(m, "name"), str(m, "description"), bool(m, "mandatory"),
                    str(m, "format"), str(m, "pageLimit"), str(m, "notes")
            );
        }
    }

    // CR-033: FACTOR>Subfactor 정밀추출
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record FactorDto(String factorId, String factorTitle, List<SubfactorDto> subfactors) {
        @SuppressWarnings("unchecked")
        public static FactorDto from(Map<String, Object> m) {
            List<SubfactorDto> subs = null;
            Object raw = m.get("subfactors");
            if (raw instanceof List<?> list) {
                subs = list.stream().filter(e -> e instanceof Map)
                        .map(e -> SubfactorDto.from((Map<String, Object>) e)).toList();
            }
            return new FactorDto(str(m, "factorId"), str(m, "factorTitle"), subs);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SubfactorDto(
            String subfactorId, String name, String description, String fulfillmentParty,
            Boolean mandatory, String format, String pageLimit, String sourceRef, String notes
    ) {
        public static SubfactorDto from(Map<String, Object> m) {
            return new SubfactorDto(
                    str(m, "subfactorId"), str(m, "name"), str(m, "description"), str(m, "fulfillmentParty"),
                    bool(m, "mandatory"), str(m, "format"), str(m, "pageLimit"), str(m, "sourceRef"), str(m, "notes")
            );
        }
    }

    // ── Document Formats ─────────────────────────────────

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record DocumentFormatsDto(
            String generalInstructions,
            List<FormatSection> formats,
            String submissionMethod
    ) {
        @SuppressWarnings("unchecked")
        public static DocumentFormatsDto from(Map<String, Object> map) {
            if (map == null || map.isEmpty()) return null;
            List<FormatSection> sections = null;
            Object raw = map.get("formats");
            if (raw instanceof List<?> list) {
                sections = list.stream()
                        .filter(e -> e instanceof Map)
                        .map(e -> FormatSection.from((Map<String, Object>) e))
                        .toList();
            }
            return new DocumentFormatsDto(
                    str(map, "generalInstructions"),
                    sections,
                    str(map, "submissionMethod")
            );
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record FormatSection(
            String section, String description, String pageLimit,
            String fileFormat, String fontRequirements, List<String> otherRequirements
    ) {
        public static FormatSection from(Map<String, Object> m) {
            List<String> others = null;
            Object raw = m.get("otherRequirements");
            if (raw instanceof List<?> list) {
                others = list.stream().map(Object::toString).toList();
            }
            return new FormatSection(
                    str(m, "section"), str(m, "description"), str(m, "pageLimit"),
                    str(m, "fileFormat"), str(m, "fontRequirements"), others
            );
        }
    }

    // ── util ─────────────────────────────────────────────

    private static String str(Map<String, Object> map, String key) {
        if (map == null) return null;
        Object v = map.get(key);
        if (v == null) return null;
        String s = v.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private static Boolean bool(Map<String, Object> map, String key) {
        if (map == null) return null;
        Object v = map.get(key);
        if (v instanceof Boolean b) return b;
        if (v == null) return null;
        return Boolean.parseBoolean(v.toString());
    }
}
