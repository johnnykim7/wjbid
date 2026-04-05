package com.biddingagency.controller.admin;

import com.biddingagency.domain.document.entity.DocumentTemplate;
import com.biddingagency.domain.document.entity.DocumentType;
import com.biddingagency.domain.document.service.DocumentTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 문서 템플릿 Admin API
 * 어드민이 Claude가 사용할 문서 템플릿을 등록·조회·비활성화합니다.
 */
@RestController
@RequestMapping("/admin/document-templates")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class DocumentTemplateAdminController {

    private final DocumentTemplateService documentTemplateService;

    /** 전체 활성 템플릿 목록 */
    @GetMapping
    public ResponseEntity<List<DocumentTemplate>> findAll() {
        return ResponseEntity.ok(documentTemplateService.findAll());
    }

    /** 문서 타입별 버전 이력 */
    @GetMapping("/{documentType}")
    public ResponseEntity<List<DocumentTemplate>> findByDocumentType(
        @PathVariable String documentType
    ) {
        DocumentType type = DocumentType.valueOf(documentType.toUpperCase());
        return ResponseEntity.ok(documentTemplateService.findByDocumentType(type));
    }

    /** 새 템플릿 등록 */
    @PostMapping
    public ResponseEntity<DocumentTemplate> create(@RequestBody CreateTemplateRequest request) {
        DocumentType type = DocumentType.valueOf(request.documentType().toUpperCase());
        DocumentTemplate saved = documentTemplateService.save(
            request.templateName(),
            type,
            request.contentJson()
        );
        return ResponseEntity.ok(saved);
    }

    /** 템플릿 수정 (새 버전 생성) */
    @PatchMapping("/{id}")
    public ResponseEntity<DocumentTemplate> update(
            @PathVariable UUID id,
            @RequestBody UpdateTemplateRequest request) {
        DocumentTemplate updated = documentTemplateService.update(
            id, request.templateName(), request.contentJson(), request.description());
        return ResponseEntity.ok(updated);
    }

    /** 템플릿 비활성화 */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        documentTemplateService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    record CreateTemplateRequest(
        String templateName,
        String documentType,
        Map<String, Object> contentJson
    ) {}

    record UpdateTemplateRequest(
        String templateName,
        Map<String, Object> contentJson,
        String description
    ) {}
}
