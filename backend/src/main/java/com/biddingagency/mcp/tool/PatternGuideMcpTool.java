package com.biddingagency.mcp.tool;

import com.biddingagency.domain.rfp.entity.IndustryType;
import com.biddingagency.domain.rfp.service.PatternExtractionService;
import com.biddingagency.domain.rfp.service.ReferenceSampleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * MCP 도구: 공고유형 패턴 추출 + 작성 참조 (CR-013 재설계).
 * - get_reference_samples: 공고유형에 매칭되는 성공 제안서들의 원본 파일 메타 + 다운로드 URL.
 *   (Aimbase가 parse_document(url)로 직접 파싱 — 패턴 추출 및 작성 시 few-shot 참조에 사용)
 * - save_pattern_guide: 추출 결과 콜백 저장 (출처 보호 = BIZ-017)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PatternGuideMcpTool {

    private final PatternExtractionService patternExtractionService;
    private final ReferenceSampleService referenceSampleService;
    private final ObjectMapper objectMapper;

    // ─── 도구 정의 ────────────────────────────────────────────────────────

    public static final List<Map<String, Object>> TOOL_DEFINITIONS = List.of(
        Map.of(
            "name", "get_reference_samples",
            "description", "특정 공고유형(industryType)에 매칭되는 성공 제안서들의 원본 파일 메타와 다운로드 URL을 조회합니다. " +
                    "각 파일의 fileName이 섹션 태그(예: 'FACTOR 3.PAST PERFORMANCE.pdf')이며, downloadUrl을 parse_document(url=...)로 " +
                    "필요한 부분만 읽어 패턴 추출 또는 제안서 작성의 few-shot 참조로 사용하세요. (사실은 복붙 금지 — 형식·전략만 참고)",
            "inputSchema", Map.of(
                "type", "object",
                "properties", Map.of(
                    "industryType", Map.of("type", "string", "description", "공고유형 (예: GROUND_MAINTENANCE, CUSTODIAL, HVAC, LAUNDRY, WASTE)")
                ),
                "required", List.of("industryType")
            )
        ),
        Map.of(
            "name", "save_pattern_guide",
            "description", "공고유형별 패턴 가이드 추출 결과를 저장합니다 (Aimbase 워크플로우 콜백용). " +
                    "source=HUMAN_EDITED인 가이드는 보호되어 저장이 스킵됩니다 (BIZ-017).",
            "inputSchema", Map.of(
                "type", "object",
                "properties", Map.of(
                    "industryType", Map.of("type", "string", "description", "공고유형"),
                    "sampleCount", Map.of("type", "integer", "description", "추출에 사용한 건수"),
                    "guide", Map.ofEntries(
                        Map.entry("type", "object"),
                        Map.entry("description", "추출된 패턴 가이드"),
                        Map.entry("properties", Map.of(
                            "skeleton", Map.of("type", "array", "description", "성공 골격 블록 목록 [{title, description}]"),
                            "checklist", Map.of("type", "array", "description", "성공 패턴 규칙 체크리스트 (string 목록)"),
                            "taboos", Map.of("type", "array", "description", "금기 사항 목록 (string 목록)"),
                            "notes", Map.of("type", "string", "description", "한계/검수 포인트")
                        ))
                    )
                ),
                "required", List.of("industryType", "guide")
            )
        )
    );

    // ─── 도구 실행 ────────────────────────────────────────────────────────

    public String getReferenceSamples(Map<String, Object> args) {
        IndustryType industryType = parseIndustry(args.get("industryType"));
        if (industryType == null) {
            throw new IllegalArgumentException("industryType은 필수입니다");
        }

        List<Map<String, Object>> samples = referenceSampleService.collect(industryType);

        // FOREACH용 평탄 파일 목록 — 워크플로우가 단일 List로 순회해 parse_document(url) 발췌.
        // samples[].files[] 2단 중첩을 평탄화. 각 항목에 출처 식별용 rfpSampleId/opportunityNo 포함.
        List<Map<String, Object>> fileUrls = new ArrayList<>();
        for (Map<String, Object> sample : samples) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> files = (List<Map<String, Object>>) sample.get("files");
            if (files == null) continue;
            for (Map<String, Object> f : files) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("rfpSampleId", sample.get("rfpSampleId"));
                entry.put("opportunityNo", sample.get("opportunityNo"));
                entry.put("fileName", f.get("fileName"));
                entry.put("downloadUrl", f.get("downloadUrl"));
                fileUrls.add(entry);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("industryType", industryType.name());
        result.put("totalSamples", samples.size());
        result.put("totalFiles", fileUrls.size());
        result.put("samples", samples);
        result.put("fileUrls", fileUrls);
        return toJson(result);
    }

    @SuppressWarnings("unchecked")
    public String savePatternGuide(Map<String, Object> args) {
        IndustryType industryType = parseIndustry(args.get("industryType"));
        if (industryType == null) {
            throw new IllegalArgumentException("industryType은 필수입니다");
        }
        Integer sampleCount = args.get("sampleCount") != null ? ((Number) args.get("sampleCount")).intValue() : null;
        Map<String, Object> guide = args.containsKey("guide") ? (Map<String, Object>) args.get("guide") : null;

        patternExtractionService.savePatternGuide(industryType, guide, sampleCount);

        log.info("MCP save_pattern_guide: industryType={}, sampleCount={}", industryType, sampleCount);
        return toJson(Map.of(
            "industryType", industryType.name(),
            "status", "SAVED",
            "message", "패턴 가이드가 저장되었습니다."
        ));
    }

    // ─── 유틸 ─────────────────────────────────────────────────────────────

    private IndustryType parseIndustry(Object raw) {
        if (raw == null || ((String) raw).isBlank()) return null;
        return IndustryType.valueOf((String) raw);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }
}
