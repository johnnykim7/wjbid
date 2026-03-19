package com.biddingagency.mcp.tool;

import com.biddingagency.domain.bid.entity.BidRequest;
import com.biddingagency.domain.bid.service.BidRequestService;
import com.biddingagency.domain.document.entity.BidDocument;
import com.biddingagency.domain.document.entity.BidDocumentVersion;
import com.biddingagency.domain.document.entity.DocumentType;
import com.biddingagency.domain.document.service.BidDocumentService;
import com.biddingagency.domain.document.service.DocumentVersionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * MCP 도구: 입찰 요청/문서 관련
 *
 * - get_bid_request: 입찰 요청 상세 조회
 * - save_document_version: 생성된 문서 저장 (신규 또는 버전 추가)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentMcpTool {

    private final BidRequestService bidRequestService;
    private final BidDocumentService bidDocumentService;
    private final DocumentVersionService documentVersionService;
    private final ObjectMapper objectMapper;

    // ─── 도구 정의 ────────────────────────────────────────────────────────

    public static final List<Map<String, Object>> TOOL_DEFINITIONS = List.of(
        Map.of(
            "name", "get_bid_request",
            "description", "입찰 요청의 현재 상태, 연결된 공고 ID, 담당자 정보를 조회합니다.",
            "inputSchema", Map.of(
                "type", "object",
                "properties", Map.of(
                    "bidRequestId", Map.of("type", "string", "description", "입찰 요청 UUID")
                ),
                "required", List.of("bidRequestId")
            )
        ),
        Map.of(
            "name", "save_document_version",
            "description", "LLM이 생성한 문서를 입찰 요청에 저장합니다. 해당 문서 타입이 없으면 새로 생성하고, 있으면 새 버전을 추가합니다.",
            "inputSchema", Map.of(
                "type", "object",
                "properties", Map.of(
                    "bidRequestId",  Map.of("type", "string", "description", "입찰 요청 UUID"),
                    "documentType",  Map.of("type", "string",
                        "description", "COVER_LETTER | TECHNICAL_PROPOSAL | PAST_PERFORMANCE | COMPANY_PROFILE | COMPLIANCE_MATRIX | PRICING_SUMMARY | OTHER"),
                    "contentJson",   Map.of("type", "object", "description", "TipTap JSON 형식의 문서 내용"),
                    "changeSummary", Map.of("type", "string", "description", "변경 요약 (optional)"),
                    "editorId",      Map.of("type", "string", "description", "편집자 UUID (optional, 없으면 시스템 UUID 사용)")
                ),
                "required", List.of("bidRequestId", "documentType", "contentJson")
            )
        )
    );

    // ─── 도구 실행 ────────────────────────────────────────────────────────

    public String getBidRequest(Map<String, Object> args) {
        UUID bidRequestId = UUID.fromString((String) args.get("bidRequestId"));
        BidRequest bidRequest = bidRequestService.findByIdWithDetails(bidRequestId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", bidRequest.getId().toString());
        result.put("state", bidRequest.getState().name());
        result.put("stateDisplay", bidRequest.getStateDisplay());
        result.put("canEditDocuments", bidRequest.canEditDocuments());
        result.put("requiresClientAction", bidRequest.requiresClientAction());
        result.put("opportunityId", bidRequest.getOpportunity().getId().toString());
        result.put("opportunityTitle", bidRequest.getOpportunity().getTitle());
        result.put("opportunityOrg", bidRequest.getOpportunity().getOrganizationName());
        result.put("memberId", bidRequest.getMember().getId().toString());
        result.put("assignedTo", bidRequest.getAssignedTo() != null ? bidRequest.getAssignedTo().toString() : null);
        result.put("submittedAt", bidRequest.getSubmittedAt() != null ? bidRequest.getSubmittedAt().toString() : null);

        // 현재 문서 목록
        List<BidDocument> docs = bidDocumentService.findByBidRequest(bidRequestId);
        List<Map<String, Object>> docList = docs.stream().map(doc -> Map.<String, Object>of(
            "id", doc.getId().toString(),
            "documentType", doc.getDocumentType().name(),
            "status", doc.getStatus().name(),
            "currentVersionNo", doc.getCurrentVersionNo()
        )).toList();
        result.put("documents", docList);

        return toJson(result);
    }

    @Transactional
    public String saveDocumentVersion(Map<String, Object> args) {
        UUID bidRequestId = UUID.fromString((String) args.get("bidRequestId"));
        String documentTypeStr = (String) args.get("documentType");
        Map<String, Object> contentJson = castMap(args.get("contentJson"));
        String changeSummary = (String) args.getOrDefault("changeSummary", "LLM 생성 문서");
        UUID editorId = args.containsKey("editorId")
            ? UUID.fromString((String) args.get("editorId"))
            : new UUID(0L, 0L); // system

        DocumentType documentType;
        try {
            documentType = DocumentType.valueOf(documentTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("알 수 없는 documentType: " + documentTypeStr
                + ". 사용 가능: " + Arrays.toString(DocumentType.values()));
        }

        // 기존 문서 여부 확인 후 생성 or 버전 추가
        BidDocumentVersion version;
        try {
            BidDocument existing = bidDocumentService.findByBidRequestAndType(bidRequestId, documentType);
            version = documentVersionService.saveVersion(existing.getId(), contentJson, changeSummary, editorId);
            log.info("MCP save_document_version: 새 버전 추가 documentId={}, version={}", existing.getId(), version.getVersionNo());
        } catch (Exception e) {
            // 문서가 없으면 신규 생성
            BidDocument newDoc = documentVersionService.createDocument(bidRequestId, documentType, contentJson, editorId);
            version = documentVersionService.getLatestVersion(newDoc.getId());
            log.info("MCP save_document_version: 신규 문서 생성 documentId={}", newDoc.getId());
        }

        Map<String, Object> result = Map.of(
            "bidRequestId", bidRequestId.toString(),
            "documentType", documentType.name(),
            "documentId", version.getDocument().getId().toString(),
            "versionNo", version.getVersionNo(),
            "versionLabel", version.getVersionLabel(),
            "message", documentType.name() + " 문서 " + version.getVersionLabel() + " 저장 완료"
        );
        return toJson(result);
    }

    // ─── 유틸 ─────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object obj) {
        if (obj instanceof Map) {
            return (Map<String, Object>) obj;
        }
        return Map.of();
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }
}
