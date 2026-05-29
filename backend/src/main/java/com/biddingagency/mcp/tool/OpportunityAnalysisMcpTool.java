package com.biddingagency.mcp.tool;

import com.biddingagency.domain.bid.entity.BidRequest;
import com.biddingagency.domain.bid.entity.BidRequestState;
import com.biddingagency.domain.bid.repository.BidRequestRepository;
import com.biddingagency.domain.document.entity.BidDocument;
import com.biddingagency.domain.document.repository.BidDocumentRepository;
import com.biddingagency.domain.notice.entity.Notice;
import com.biddingagency.domain.notice.service.NoticeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * MCP 도구: 공고문(Notice) 한글화/요약 결과 저장·조회 + 과거 제출 이력 (CR-003, CR-016).
 * 한글화 결과는 noticeId 기준으로 저장 (원본 1:N 공고문).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpportunityAnalysisMcpTool {

    private final NoticeService noticeService;
    private final BidRequestRepository bidRequestRepository;
    private final BidDocumentRepository bidDocumentRepository;
    private final ObjectMapper objectMapper;

    // ─── 도구 정의 ────────────────────────────────────────────────────────

    public static final List<Map<String, Object>> TOOL_DEFINITIONS = List.of(
        Map.of(
            "name", "get_opportunity_analysis",
            "description", "공고문(Notice) 한글화/요약 결과를 조회합니다 (요약, 문서양식, 필요서류, LLM 프롬프트 프리셋).",
            "inputSchema", Map.of(
                "type", "object",
                "properties", Map.of(
                    "noticeId", Map.of("type", "string", "description", "공고문 UUID")
                ),
                "required", List.of("noticeId")
            )
        ),
        Map.of(
            "name", "save_opportunity_analysis",
            "description", "공고문(Notice) 한글화/요약 결과를 저장합니다 (Aimbase 워크플로우 콜백용).",
            "inputSchema", Map.of(
                "type", "object",
                "properties", Map.of(
                    "noticeId", Map.of("type", "string", "description", "공고문 UUID"),
                    "koreanTitle", Map.of("type", "string", "description", "한글화된 공고 제목"),
                    "summary", Map.ofEntries(
                        Map.entry("type", "object"),
                        Map.entry("description", "공고 요약본"),
                        Map.entry("properties", Map.of(
                            "overview", Map.of("type", "string", "description", "공고 핵심 요약 (2-3문장)"),
                            "scope", Map.of("type", "string", "description", "작업 범위 (Scope of Work)"),
                            "eligibility", Map.of("type", "string", "description", "참여 자격 요건"),
                            "evaluationCriteria", Map.of("type", "string", "description", "평가 기준"),
                            "keyDates", Map.of("type", "array", "description", "주요 일정 [{label, date, note}]"),
                            "budgetInfo", Map.of("type", "string", "description", "예산 정보"),
                            "specialNotes", Map.of("type", "array", "description", "특이사항 목록")
                        ))
                    ),
                    "documentFormats", Map.ofEntries(
                        Map.entry("type", "object"),
                        Map.entry("description", "문서 양식 정보"),
                        Map.entry("properties", Map.of(
                            "generalInstructions", Map.of("type", "string", "description", "전반적 양식 안내"),
                            "formats", Map.of("type", "array", "description", "섹션별 양식 [{section, description, pageLimit, fileFormat, fontRequirements}]"),
                            "submissionMethod", Map.of("type", "string", "description", "제출 방법")
                        ))
                    ),
                    "requiredDocuments", Map.ofEntries(
                        Map.entry("type", "object"),
                        Map.entry("description", "필요 서류 목록"),
                        Map.entry("properties", Map.of(
                            "documents", Map.of("type", "array", "description", "서류 목록 [{name, description, mandatory, format, pageLimit, notes}]")
                        ))
                    ),
                    "llmPromptPreset", Map.of("type", "object", "description", "LLM 프롬프트 프리셋 (내부용)")
                ),
                "required", List.of("noticeId")
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
        UUID noticeId = UUID.fromString((String) args.get("noticeId"));

        Notice notice;
        try {
            notice = noticeService.findById(noticeId);
        } catch (IllegalArgumentException e) {
            return toJson(Map.of("noticeId", noticeId.toString(), "status", "NOT_FOUND"));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("noticeId", noticeId.toString());
        result.put("status", notice.getGenerationStatus().name());
        result.put("koreanTitle", notice.getKoreanTitle());
        result.put("summary", notice.getSummaryJson());
        result.put("documentFormats", notice.getDocumentFormatsJson());
        result.put("requiredDocuments", notice.getRequiredDocumentsJson());
        result.put("llmPromptPreset", notice.getLlmPromptPresetJson());
        result.put("analyzedAt", notice.getAnalyzedAt() != null ? notice.getAnalyzedAt().toString() : null);

        return toJson(result);
    }

    @SuppressWarnings("unchecked")
    @Transactional
    public String saveOpportunityAnalysis(Map<String, Object> args) {
        UUID noticeId = UUID.fromString((String) args.get("noticeId"));
        String koreanTitle = (String) args.get("koreanTitle");

        Map<String, Object> summary = args.containsKey("summary") ? (Map<String, Object>) args.get("summary") : null;
        Map<String, Object> documentFormats = args.containsKey("documentFormats") ? (Map<String, Object>) args.get("documentFormats") : null;
        Map<String, Object> requiredDocuments = args.containsKey("requiredDocuments") ? (Map<String, Object>) args.get("requiredDocuments") : null;
        Map<String, Object> llmPromptPreset = args.containsKey("llmPromptPreset") ? (Map<String, Object>) args.get("llmPromptPreset") : null;

        Notice notice = noticeService.saveResult(
                noticeId, koreanTitle, summary, documentFormats, requiredDocuments, llmPromptPreset);

        log.info("MCP save_opportunity_analysis: noticeId={}, status={}", noticeId, notice.getGenerationStatus());

        return toJson(Map.of(
            "noticeId", noticeId.toString(),
            "status", notice.getGenerationStatus().name(),
            "message", "공고문 한글화 결과가 저장되었습니다."
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
