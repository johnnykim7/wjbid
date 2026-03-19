package com.biddingagency.mcp.tool;

import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.entity.OpportunityRequirementItem;
import com.biddingagency.domain.opportunity.entity.RequirementCategory;
import com.biddingagency.domain.opportunity.repository.OpportunityRequirementItemRepository;
import com.biddingagency.domain.opportunity.service.OpportunityService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * MCP 도구: 요구사항(Requirement) 관련
 *
 * - save_requirements: 공고 요구사항 일괄 저장 (기존 삭제 후 재저장)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RequirementMcpTool {

    private final OpportunityService opportunityService;
    private final OpportunityRequirementItemRepository requirementItemRepository;
    private final ObjectMapper objectMapper;

    // ─── 도구 정의 ────────────────────────────────────────────────────────

    public static final List<Map<String, Object>> TOOL_DEFINITIONS = List.of(
        Map.of(
            "name", "save_requirements",
            "description", "LLM이 추출한 요구사항을 공고에 저장합니다. 기존 요구사항을 모두 삭제하고 새로 저장합니다.",
            "inputSchema", Map.of(
                "type", "object",
                "properties", Map.of(
                    "opportunityId", Map.of("type", "string", "description", "공고 UUID"),
                    "requirements", Map.of(
                        "type", "array",
                        "description", "저장할 요구사항 목록",
                        "items", Map.of(
                            "type", "object",
                            "properties", Map.of(
                                "category",    Map.of("type", "string",
                                    "description", "DOCUMENT | FORMAT | SUBMISSION | DEADLINE | ELIGIBILITY | TECHNICAL | OTHER"),
                                "title",       Map.of("type", "string", "description", "요구사항 제목 (500자 이내)"),
                                "description", Map.of("type", "string", "description", "요구사항 상세 설명"),
                                "isBlocker",   Map.of("type", "boolean", "description", "필수 요건 여부"),
                                "metadata",    Map.of("type", "object", "description", "추가 메타데이터 (참조 페이지 등)")
                            ),
                            "required", List.of("category", "title")
                        )
                    )
                ),
                "required", List.of("opportunityId", "requirements")
            )
        )
    );

    // ─── 도구 실행 ────────────────────────────────────────────────────────

    @Transactional
    public String saveRequirements(Map<String, Object> args) {
        UUID opportunityId = UUID.fromString((String) args.get("opportunityId"));
        List<Map<String, Object>> reqList = castList(args.get("requirements"));

        Opportunity opportunity = opportunityService.findById(opportunityId);

        // 기존 요구사항 전체 삭제
        requirementItemRepository.deleteByOpportunityId(opportunityId);

        // 새 요구사항 저장
        List<OpportunityRequirementItem> items = reqList.stream().map(req -> {
            String categoryStr = (String) req.getOrDefault("category", "OTHER");
            RequirementCategory category;
            try {
                category = RequirementCategory.valueOf(categoryStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                category = RequirementCategory.OTHER;
            }

            String title = (String) req.get("title");
            if (title == null || title.isBlank()) {
                throw new IllegalArgumentException("요구사항 title은 필수입니다");
            }
            if (title.length() > 500) {
                title = title.substring(0, 500);
            }

            String description = (String) req.getOrDefault("description", "");
            Boolean isBlocker = req.containsKey("isBlocker") ? (Boolean) req.get("isBlocker") : false;

            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = req.containsKey("metadata")
                ? (Map<String, Object>) req.get("metadata")
                : Map.of();

            return OpportunityRequirementItem.builder()
                .opportunity(opportunity)
                .category(category)
                .title(title)
                .description(description)
                .isBlocker(isBlocker)
                .requirementJson(metadata)
                .build();
        }).collect(Collectors.toList());

        requirementItemRepository.saveAll(items);
        log.info("MCP save_requirements: opportunityId={}, saved={}", opportunityId, items.size());

        Map<String, Object> result = Map.of(
            "opportunityId", opportunityId.toString(),
            "savedCount", items.size(),
            "message", items.size() + "개 요구사항이 저장되었습니다."
        );
        return toJson(result);
    }

    // ─── 유틸 ─────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castList(Object obj) {
        if (obj instanceof List) {
            return (List<Map<String, Object>>) obj;
        }
        return List.of();
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }
}
