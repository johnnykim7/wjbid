package com.biddingagency.mcp.tool;

import com.biddingagency.domain.document.entity.DocumentTemplate;
import com.biddingagency.domain.document.entity.DocumentType;
import com.biddingagency.domain.document.service.DocumentTemplateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * MCP 도구: 문서 템플릿 조회
 *
 * - get_document_template: 문서 타입별 TipTap JSON 템플릿 반환
 */
@Component
@RequiredArgsConstructor
public class DocumentTemplateMcpTool {

    private final DocumentTemplateService documentTemplateService;
    private final ObjectMapper objectMapper;

    // ─── 도구 정의 ────────────────────────────────────────────────────────

    public static final List<Map<String, Object>> TOOL_DEFINITIONS = List.of(
        Map.of(
            "name", "get_document_template",
            "description", "문서 타입에 맞는 TipTap JSON 템플릿을 조회합니다. 문서를 생성할 때 반드시 이 템플릿 구조를 기반으로 작성하세요.",
            "inputSchema", Map.of(
                "type", "object",
                "properties", Map.of(
                    "documentType", Map.of(
                        "type", "string",
                        "description", "COVER_LETTER | TECHNICAL_PROPOSAL | PAST_PERFORMANCE | COMPANY_PROFILE | COMPLIANCE_MATRIX | PRICING_SUMMARY | OTHER"
                    )
                ),
                "required", List.of("documentType")
            )
        )
    );

    // ─── 도구 실행 ────────────────────────────────────────────────────────

    public String getDocumentTemplate(Map<String, Object> args) {
        String documentTypeStr = (String) args.get("documentType");

        DocumentType documentType;
        try {
            documentType = DocumentType.valueOf(documentTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("알 수 없는 documentType: " + documentTypeStr
                + ". 사용 가능: " + Arrays.toString(DocumentType.values()));
        }

        DocumentTemplate template = documentTemplateService.getActiveTemplate(documentType);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("templateId", template.getId().toString());
        result.put("templateName", template.getTemplateName());
        result.put("documentType", template.getDocumentType().name());
        result.put("templateVersion", template.getTemplateVersion());
        result.put("contentJson", template.getContentJson());

        return toJson(result);
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
