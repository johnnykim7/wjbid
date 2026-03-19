package com.biddingagency.mcp.tool;

import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.entity.OpportunityRequirementItem;
import com.biddingagency.domain.opportunity.repository.OpportunityRequirementItemRepository;
import com.biddingagency.domain.opportunity.service.OpportunityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * MCP 도구: 공고(Opportunity) 관련
 *
 * - get_opportunity: 공고 상세 조회
 * - search_opportunities: 키워드로 공고 검색
 * - get_opportunity_requirements: 공고 요구사항 목록 조회
 */
@Component
@RequiredArgsConstructor
public class OpportunityMcpTool {

    private final OpportunityService opportunityService;
    private final OpportunityRequirementItemRepository requirementItemRepository;

    // ─── 도구 정의 ────────────────────────────────────────────────────────

    public static final List<Map<String, Object>> TOOL_DEFINITIONS = List.of(
        Map.of(
            "name", "get_opportunity",
            "description", "입찰 공고 상세 정보를 조회합니다. 공고 ID로 제목, 기관명, 마감일, 공고 링크 등을 반환합니다.",
            "inputSchema", Map.of(
                "type", "object",
                "properties", Map.of(
                    "opportunityId", Map.of("type", "string", "description", "공고 UUID")
                ),
                "required", List.of("opportunityId")
            )
        ),
        Map.of(
            "name", "search_opportunities",
            "description", "키워드로 입찰 공고를 검색합니다. 제목 기준으로 검색하며 최신순으로 반환합니다.",
            "inputSchema", Map.of(
                "type", "object",
                "properties", Map.of(
                    "keyword", Map.of("type", "string", "description", "검색 키워드"),
                    "limit",   Map.of("type", "integer", "description", "최대 결과 수 (기본 10, 최대 50)")
                ),
                "required", List.of("keyword")
            )
        ),
        Map.of(
            "name", "get_opportunity_requirements",
            "description", "공고에 저장된 요구사항 목록을 조회합니다. 카테고리, 필수 여부, 상세 설명을 포함합니다.",
            "inputSchema", Map.of(
                "type", "object",
                "properties", Map.of(
                    "opportunityId", Map.of("type", "string", "description", "공고 UUID")
                ),
                "required", List.of("opportunityId")
            )
        )
    );

    // ─── 도구 실행 ────────────────────────────────────────────────────────

    public String getOpportunity(Map<String, Object> args) {
        UUID opportunityId = UUID.fromString((String) args.get("opportunityId"));
        Opportunity opp = opportunityService.findById(opportunityId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", opp.getId().toString());
        result.put("noticeId", opp.getNoticeId());
        result.put("title", opp.getTitle());
        result.put("type", opp.getType());
        result.put("organizationName", opp.getOrganizationName());
        result.put("postedDate", opp.getPostedDate() != null ? opp.getPostedDate().toString() : null);
        result.put("responseDeadline", opp.getResponseDeadline() != null ? opp.getResponseDeadline().toString() : null);
        result.put("deadlinePassed", opp.isDeadlinePassed());
        result.put("active", opp.getActive());
        result.put("uiLink", opp.getUiLink());
        result.put("descriptionLink", opp.getDescriptionLink());

        return toJson(result);
    }

    public String searchOpportunities(Map<String, Object> args) {
        String keyword = (String) args.get("keyword");
        int limit = args.containsKey("limit") ? ((Number) args.get("limit")).intValue() : 10;
        limit = Math.min(limit, 50);

        Page<Opportunity> page = opportunityService.searchByKeyword(keyword, PageRequest.of(0, limit));

        List<Map<String, Object>> items = page.getContent().stream().map(opp -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", opp.getId().toString());
            item.put("noticeId", opp.getNoticeId());
            item.put("title", opp.getTitle());
            item.put("organizationName", opp.getOrganizationName());
            item.put("responseDeadline", opp.getResponseDeadline() != null ? opp.getResponseDeadline().toString() : null);
            item.put("active", opp.getActive());
            return item;
        }).collect(Collectors.toList());

        Map<String, Object> result = Map.of(
            "keyword", keyword,
            "totalFound", page.getTotalElements(),
            "opportunities", items
        );
        return toJson(result);
    }

    public String getOpportunityRequirements(Map<String, Object> args) {
        UUID opportunityId = UUID.fromString((String) args.get("opportunityId"));
        List<OpportunityRequirementItem> items = requirementItemRepository.findByOpportunityId(opportunityId);

        List<Map<String, Object>> requirements = items.stream().map(item -> {
            Map<String, Object> req = new LinkedHashMap<>();
            req.put("id", item.getId().toString());
            req.put("category", item.getCategory().name());
            req.put("title", item.getTitle());
            req.put("description", item.getDescription());
            req.put("isBlocker", item.getIsBlocker());
            req.put("isVerified", item.getIsVerified());
            req.put("metadata", item.getRequirementJson());
            return req;
        }).collect(Collectors.toList());

        Map<String, Object> result = Map.of(
            "opportunityId", opportunityId.toString(),
            "count", requirements.size(),
            "requirements", requirements
        );
        return toJson(result);
    }

    // ─── 유틸 ─────────────────────────────────────────────────────────────

    private String toJson(Object obj) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }
}
