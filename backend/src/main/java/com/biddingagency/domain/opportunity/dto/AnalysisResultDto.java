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
        DocumentFormatsDto documentFormats
) {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** CR-016: 한글화 결과는 Notice가 보유 */
    public static AnalysisResultDto fromNotice(Notice notice) {
        if (notice == null) return null;

        String status = notice.getGenerationStatus() != null ? notice.getGenerationStatus().name() : null;
        String analyzedAt = notice.getAnalyzedAt() != null ? notice.getAnalyzedAt().format(FMT) : null;

        if (notice.getGenerationStatus() != NoticeGenerationStatus.COMPLETED) {
            return new AnalysisResultDto(status, analyzedAt, null, null, null);
        }

        return new AnalysisResultDto(
                status,
                analyzedAt,
                SummaryDto.from(notice.getSummaryJson()),
                RequiredDocumentsDto.from(notice.getRequiredDocumentsJson()),
                DocumentFormatsDto.from(notice.getDocumentFormatsJson())
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
    public record RequiredDocumentsDto(List<RequiredDocumentItem> documents) {
        @SuppressWarnings("unchecked")
        public static RequiredDocumentsDto from(Map<String, Object> map) {
            if (map == null || map.isEmpty()) return null;
            Object raw = map.get("documents");
            if (!(raw instanceof List<?> list)) return new RequiredDocumentsDto(Collections.emptyList());
            List<RequiredDocumentItem> items = list.stream()
                    .filter(e -> e instanceof Map)
                    .map(e -> RequiredDocumentItem.from((Map<String, Object>) e))
                    .toList();
            return new RequiredDocumentsDto(items);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record RequiredDocumentItem(
            String name, String description, Boolean mandatory,
            String format, String pageLimit, String notes
    ) {
        public static RequiredDocumentItem from(Map<String, Object> m) {
            Boolean mandatory = null;
            Object v = m.get("mandatory");
            if (v instanceof Boolean b) mandatory = b;
            else if (v != null) mandatory = Boolean.parseBoolean(v.toString());
            return new RequiredDocumentItem(
                    str(m, "name"), str(m, "description"), mandatory,
                    str(m, "format"), str(m, "pageLimit"), str(m, "notes")
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
}
