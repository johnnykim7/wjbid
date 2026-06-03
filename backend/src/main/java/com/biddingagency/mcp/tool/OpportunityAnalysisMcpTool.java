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
                        Map.entry("description", "필요 서류·제출물 (CR-033: FACTOR>Subfactor 정밀추출). Instructions to Offerors(52.212-1 Addendum)·PWS를 읽어 추출"),
                        Map.entry("properties", Map.of(
                            // CR-033: FACTOR>Subfactor 계층. Solicitation 원문의 평가축이자 제출물 골격.
                            "factors", Map.of("type", "array", "description",
                                "평가·제출 FACTOR 트리 [{factorId(예 'I'), factorTitle(예 'TECHNICAL'), subfactors:[{subfactorId(예 'I-1'), name, description, " +
                                "fulfillmentParty('CLIENT_UPLOAD'=고객업로드(사업자등록·업종허가·과거실적)/'PLATFORM_GENERATED'=플랫폼생성(기술·가격제안서)/'SYSTEM_FORM'=시스템양식(SF1449·SF30·52.212-3)), " +
                                "mandatory(boolean), format, pageLimit, sourceRef(근거 위치 예 'Addendum to 52.212-1 §4.1 Sub-Factor 1'), notes}]}]"),
                            // 하위호환: 기존 평면 슬롯. factors[]와 함께 채워 무중단 전환(CLIENT_UPLOAD subfactor를 평면화).
                            "documents", Map.of("type", "array", "description", "[하위호환] 평면 서류 목록 [{name, description, mandatory, format, pageLimit, notes}] — fulfillmentParty=CLIENT_UPLOAD 항목만 채움"),
                            // CR-033: 자격요건(서류와 같은 근거 Instructions/PWS에서 추출). 자격=제출 증빙의 동전 양면.
                            "eligibility", Map.of("type", "array", "description",
                                "참여 자격요건 [{title, description, mandatory(boolean), " +
                                "evidenceBy(이 자격을 증빙하는 서류 슬롯 name — factors의 subfactor.name과 연결, 없으면 null), " +
                                "isGate(boolean — 미충족 시 평가 제외/입찰 부적격), sourceRef(근거 위치)}]")
                        ))
                    ),
                    "llmPromptPreset", Map.of("type", "object", "description", "LLM 프롬프트 프리셋 (내부용)"),
                    "contentJson", Map.of("type", "object", "description", "TipTap JSON 본문 — NOTICE_VIEW 템플릿 골격을 채워서 반환 (PDF 풍부도). 노드 타입: noticeHeader/metaGrid/kvTable/dataTable/groupedList/calloutList 등 커스텀 + heading/paragraph/bulletList 표준")
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
        result.put("contentJson", notice.getContentJson());
        // CR-038: 교정 채팅이 근거(sourceQuote) 보고 판단·치환할 수 있게 facts 반환.
        // save 가 전체치환이므로 get→수정→save 왕복 시 facts 도 함께 실려야 소멸 안 됨.
        result.put("facts", notice.getExtractedFactsJson());
        result.put("analyzedAt", notice.getAnalyzedAt() != null ? notice.getAnalyzedAt().toString() : null);

        return toJson(result);
    }

    @SuppressWarnings("unchecked")
    @Transactional
    public String saveOpportunityAnalysis(Map<String, Object> args) {
        UUID noticeId = UUID.fromString((String) args.get("noticeId"));
        String koreanTitle = (String) args.get("koreanTitle");

        // Aimbase 워크플로우의 변수 치환이 객체를 JSON 문자열로 직렬화해서 보낼 수 있어 양쪽 호환
        Map<String, Object> summary = coerceToMap(args.get("summary"));
        Map<String, Object> documentFormats = coerceToMap(args.get("documentFormats"));
        Map<String, Object> requiredDocuments = coerceToMap(args.get("requiredDocuments"));
        Map<String, Object> llmPromptPreset = coerceToMap(args.get("llmPromptPreset"));
        Map<String, Object> contentJson = coerceToMap(args.get("contentJson"));
        // CR-038: 추출 사실(근거 부착) 배열. 없으면 null → markCompleted 가 기존값 보존.
        List<Map<String, Object>> facts = coerceToList(args.get("facts"));

        Notice notice = noticeService.saveResult(
                noticeId, koreanTitle, summary, documentFormats, requiredDocuments, llmPromptPreset, contentJson, facts);

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

    /** Aimbase 변수 치환이 객체를 JSON 문자열로 보내는 경우가 있어 양쪽 호환. */
    @SuppressWarnings("unchecked")
    private Map<String, Object> coerceToMap(Object value) {
        if (value == null) return null;
        if (value instanceof Map<?, ?> m) return (Map<String, Object>) m;
        if (value instanceof String s) {
            String trimmed = s.trim();
            if (trimmed.isEmpty() || "null".equals(trimmed)) return null;
            try {
                return objectMapper.readValue(trimmed, Map.class);
            } catch (Exception e) {
                log.warn("MCP coerceToMap: String→Map 파싱 실패, null 반환. preview={}", trimmed.substring(0, Math.min(120, trimmed.length())));
                return null;
            }
        }
        log.warn("MCP coerceToMap: 예상 외 타입 {} → null", value.getClass().getName());
        return null;
    }

    /** CR-038: facts 배열 변환. Aimbase 변수치환이 List 를 JSON 문자열로 직렬화해 보낼 수 있어 양쪽 호환. */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> coerceToList(Object value) {
        if (value == null) return null;
        if (value instanceof List<?> l) return (List<Map<String, Object>>) l;
        if (value instanceof String s) {
            String trimmed = s.trim();
            if (trimmed.isEmpty() || "null".equals(trimmed)) return null;
            try {
                return objectMapper.readValue(trimmed, List.class);
            } catch (Exception e) {
                log.warn("MCP coerceToList: String→List 파싱 실패, null 반환. preview={}", trimmed.substring(0, Math.min(120, trimmed.length())));
                return null;
            }
        }
        log.warn("MCP coerceToList: 예상 외 타입 {} → null", value.getClass().getName());
        return null;
    }
}
