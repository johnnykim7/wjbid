package com.biddingagency.mcp.tool;

import com.biddingagency.domain.bid.entity.BidRequest;
import com.biddingagency.domain.bid.entity.BidRequestState;
import com.biddingagency.domain.bid.repository.BidRequestRepository;
import com.biddingagency.domain.document.entity.BidDocument;
import com.biddingagency.domain.document.repository.BidDocumentRepository;
import com.biddingagency.domain.opportunity.entity.OpportunityAnalysis;
import com.biddingagency.domain.opportunity.service.OpportunityAnalysisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * MCP 도구: 공고 사전 분석 + 과거 제출 이력 (CR-003)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpportunityAnalysisMcpTool {

    private final OpportunityAnalysisService analysisService;
    private final BidRequestRepository bidRequestRepository;
    private final BidDocumentRepository bidDocumentRepository;
    private final ObjectMapper objectMapper;

    // ─── 도구 정의 ────────────────────────────────────────────────────────

    public static final List<Map<String, Object>> TOOL_DEFINITIONS = List.of(
        Map.of(
            "name", "get_opportunity_analysis",
            "description", "공고 사전 분석 결과를 조회합니다 (요약, 문서양식, 필요서류, LLM 프롬프트 프리셋).",
            "inputSchema", Map.of(
                "type", "object",
                "properties", Map.of(
                    "opportunityId", Map.of("type", "string", "description", "공고 UUID")
                ),
                "required", List.of("opportunityId")
            )
        ),
        Map.of(
            "name", "save_opportunity_analysis",
            "description", "공고 사전 분석 결과를 저장합니다 (Aimbase 워크플로우 콜백용).",
            "inputSchema", Map.of(
                "type", "object",
                "properties", Map.of(
                    "opportunityId", Map.of("type", "string", "description", "공고 UUID"),
                    "summary", Map.of("type", "object", "description", "공고 요약본"),
                    "documentFormats", Map.of("type", "object", "description", "문서 양식 정보"),
                    "requiredDocuments", Map.of("type", "object", "description", "필요 서류 목록"),
                    "llmPromptPreset", Map.of("type", "object", "description", "LLM 프롬프트 프리셋")
                ),
                "required", List.of("opportunityId")
            )
        ),
        Map.of(
            "name", "get_past_submissions",
            "description", "특정 회원의 과거 제출(SUBMITTED) 입찰 이력과 문서 목록을 조회합니다.",
            "inputSchema", Map.of(
                "type", "object",
                "properties", Map.of(
                    "memberId", Map.of("type", "string", "description", "회원 UUID"),
                    "limit", Map.of("type", "integer", "description", "최대 건수 (기본 10)")
                ),
                "required", List.of("memberId")
            )
        )
    );

    // ─── 도구 실행 ────────────────────────────────────────────────────────

    public String getOpportunityAnalysis(Map<String, Object> args) {
        UUID opportunityId = UUID.fromString((String) args.get("opportunityId"));

        OpportunityAnalysis analysis = analysisService.findByOpportunityId(opportunityId)
                .orElse(null);

        if (analysis == null) {
            return toJson(Map.of("opportunityId", opportunityId.toString(), "status", "NOT_FOUND"));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("opportunityId", opportunityId.toString());
        result.put("status", analysis.getStatus().name());
        result.put("summary", analysis.getSummaryJson());
        result.put("documentFormats", analysis.getDocumentFormatsJson());
        result.put("requiredDocuments", analysis.getRequiredDocumentsJson());
        result.put("llmPromptPreset", analysis.getLlmPromptPresetJson());
        result.put("analyzedAt", analysis.getAnalyzedAt() != null ? analysis.getAnalyzedAt().toString() : null);

        return toJson(result);
    }

    @SuppressWarnings("unchecked")
    @Transactional
    public String saveOpportunityAnalysis(Map<String, Object> args) {
        UUID opportunityId = UUID.fromString((String) args.get("opportunityId"));

        Map<String, Object> summary = args.containsKey("summary") ? (Map<String, Object>) args.get("summary") : null;
        Map<String, Object> documentFormats = args.containsKey("documentFormats") ? (Map<String, Object>) args.get("documentFormats") : null;
        Map<String, Object> requiredDocuments = args.containsKey("requiredDocuments") ? (Map<String, Object>) args.get("requiredDocuments") : null;
        Map<String, Object> llmPromptPreset = args.containsKey("llmPromptPreset") ? (Map<String, Object>) args.get("llmPromptPreset") : null;

        OpportunityAnalysis analysis = analysisService.saveAnalysisResult(
                opportunityId, summary, documentFormats, requiredDocuments, llmPromptPreset);

        log.info("MCP save_opportunity_analysis: opportunityId={}, status={}", opportunityId, analysis.getStatus());

        return toJson(Map.of(
            "opportunityId", opportunityId.toString(),
            "status", analysis.getStatus().name(),
            "message", "사전 분석 결과가 저장되었습니다."
        ));
    }

    public String getPastSubmissions(Map<String, Object> args) {
        UUID memberId = UUID.fromString((String) args.get("memberId"));
        int limit = args.containsKey("limit") ? ((Number) args.get("limit")).intValue() : 10;

        List<BidRequest> submitted = bidRequestRepository.findByMemberIdAndState(memberId, BidRequestState.SUBMITTED);

        List<Map<String, Object>> submissions = new ArrayList<>();
        int count = 0;
        for (BidRequest br : submitted) {
            if (count >= limit) break;

            List<BidDocument> docs = bidDocumentRepository.findByBidRequestId(br.getId());
            List<Map<String, Object>> docList = docs.stream().map(d -> Map.<String, Object>of(
                "documentId", d.getId().toString(),
                "documentType", d.getDocumentType().name(),
                "status", d.getStatus().name()
            )).toList();

            submissions.add(Map.of(
                "bidRequestId", br.getId().toString(),
                "opportunityTitle", br.getOpportunity() != null ? br.getOpportunity().getTitle() : "",
                "submittedAt", br.getSubmittedAt() != null ? br.getSubmittedAt().toString() : "",
                "documents", docList
            ));
            count++;
        }

        return toJson(Map.of(
            "memberId", memberId.toString(),
            "totalCount", submitted.size(),
            "submissions", submissions
        ));
    }

    // ─── 유틸 ─────────────────────────────────────────────────────────────

    private String toJson(Object obj) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }
}
