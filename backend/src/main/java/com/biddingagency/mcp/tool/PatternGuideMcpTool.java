package com.biddingagency.mcp.tool;

import com.biddingagency.domain.rfp.entity.IndustryType;
import com.biddingagency.domain.rfp.entity.SlotAssignment;
import com.biddingagency.domain.rfp.entity.SlotDefinition;
import com.biddingagency.domain.rfp.repository.SlotAssignmentRepository;
import com.biddingagency.domain.rfp.repository.SlotDefinitionRepository;
import com.biddingagency.domain.rfp.service.PatternExtractionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * MCP 도구: 슬롯 패턴 추출 (CR-013).
 * - get_slot_samples: 슬롯에 모인 각 건의 파일 메타 + 다운로드 URL (Aimbase가 원본을 직접 파싱)
 * - save_pattern_guide: 추출 결과 콜백 저장 (출처 보호 = BIZ-017)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PatternGuideMcpTool {

    private final SlotDefinitionRepository slotDefinitionRepository;
    private final SlotAssignmentRepository slotAssignmentRepository;
    private final PatternExtractionService patternExtractionService;
    private final ObjectMapper objectMapper;

    @Value("${app.self-base-url:http://59.8.160.12:8183/api}")
    private String selfBaseUrl;

    // ─── 도구 정의 ────────────────────────────────────────────────────────

    public static final List<Map<String, Object>> TOOL_DEFINITIONS = List.of(
        Map.of(
            "name", "get_slot_samples",
            "description", "특정 슬롯(정규화 축)에 배치된 성공 제안서들의 파일 메타와 다운로드 URL을 조회합니다. " +
                    "각 sample의 downloadUrl을 parse_document(url=...)로 파싱한 뒤 슬롯 공통 패턴을 추출하세요.",
            "inputSchema", Map.of(
                "type", "object",
                "properties", Map.of(
                    "slotCode", Map.of("type", "string", "description", "슬롯 코드 (예: PRIOR_EXPERIENCE)"),
                    "industryType", Map.of("type", "string", "description", "사업유형 필터 (선택)")
                ),
                "required", List.of("slotCode")
            )
        ),
        Map.of(
            "name", "save_pattern_guide",
            "description", "슬롯별 패턴 가이드 추출 결과를 저장합니다 (Aimbase 워크플로우 콜백용). " +
                    "source=HUMAN_EDITED인 가이드는 보호되어 저장이 스킵됩니다 (BIZ-017).",
            "inputSchema", Map.of(
                "type", "object",
                "properties", Map.of(
                    "slotCode", Map.of("type", "string", "description", "슬롯 코드"),
                    "industryType", Map.of("type", "string", "description", "사업유형 (선택, 미지정 시 공통 가이드)"),
                    "sampleCount", Map.of("type", "integer", "description", "추출에 사용한 건수"),
                    "guide", Map.ofEntries(
                        Map.entry("type", "object"),
                        Map.entry("description", "추출된 패턴 가이드 (rfp_pattern_guide_draft 구조)"),
                        Map.entry("properties", Map.of(
                            "skeleton", Map.of("type", "array", "description", "성공 골격 블록 목록 [{title, description}]"),
                            "checklist", Map.of("type", "array", "description", "성공 패턴 규칙 체크리스트 (string 목록)"),
                            "taboos", Map.of("type", "array", "description", "금기 사항 목록 (string 목록)"),
                            "notes", Map.of("type", "string", "description", "한계/검수 포인트")
                        ))
                    )
                ),
                "required", List.of("slotCode", "guide")
            )
        )
    );

    // ─── 도구 실행 ────────────────────────────────────────────────────────

    public String getSlotSamples(Map<String, Object> args) {
        String slotCode = (String) args.get("slotCode");
        IndustryType industryFilter = parseIndustry(args.get("industryType"));

        SlotDefinition slot = slotDefinitionRepository.findBySlotCode(slotCode)
                .orElseThrow(() -> new IllegalArgumentException("슬롯을 찾을 수 없습니다: " + slotCode));

        List<Map<String, Object>> samples = new ArrayList<>();
        for (SlotAssignment a : slotAssignmentRepository.findBySlotDefinitionId(slot.getId())) {
            if (industryFilter != null && a.getRfpSample().getIndustryType() != industryFilter) {
                continue;
            }
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("rfpSampleId", a.getRfpSample().getId().toString());
            s.put("opportunityNo", a.getRfpSample().getOpportunityNo());
            s.put("industryType", a.getRfpSample().getIndustryType().name());
            s.put("outcome", a.getRfpSample().getOutcome().name());
            if (a.getSampleFile() != null) {
                s.put("fileId", a.getSampleFile().getId().toString());
                s.put("fileName", a.getSampleFile().getFileName());
                s.put("contentType", a.getSampleFile().getContentType());
                s.put("downloadUrl", selfBaseUrl + "/mcp/rfp-files/" + a.getSampleFile().getId() + "/download");
            }
            if (a.getSectionText() != null) {
                s.put("sectionText", a.getSectionText());
            }
            samples.add(s);
        }

        return toJson(Map.of(
            "slotCode", slotCode,
            "slotLabel", slot.getLabelKo(),
            "totalCount", samples.size(),
            "samples", samples
        ));
    }

    @SuppressWarnings("unchecked")
    public String savePatternGuide(Map<String, Object> args) {
        String slotCode = (String) args.get("slotCode");
        IndustryType industryType = parseIndustry(args.get("industryType"));
        Integer sampleCount = args.get("sampleCount") != null ? ((Number) args.get("sampleCount")).intValue() : null;
        Map<String, Object> guide = args.containsKey("guide") ? (Map<String, Object>) args.get("guide") : null;

        patternExtractionService.savePatternGuide(slotCode, industryType, guide, sampleCount);

        log.info("MCP save_pattern_guide: slotCode={}, sampleCount={}", slotCode, sampleCount);
        return toJson(Map.of(
            "slotCode", slotCode,
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
