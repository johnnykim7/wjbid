package com.biddingagency.mcp;

import com.biddingagency.domain.opportunity.entity.AttachmentDownloadStatus;
import com.biddingagency.domain.opportunity.entity.OpportunityAttachment;
import com.biddingagency.domain.opportunity.repository.OpportunityAttachmentRepository;
import com.biddingagency.integration.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 공고 첨부 다운로드 (CR-019).
 * /mcp/** 경로로 두어 Aimbase(server-to-server, JWT 불필요)가 한글화 시 첨부를 가져가 parse_document로 직접 파싱.
 * SUCCESS(실제 저장된) 첨부만 다운로드 가능.
 */
@Slf4j
@RestController
@RequestMapping("/mcp/opportunity-attachments")
@RequiredArgsConstructor
public class OpportunityAttachmentDownloadController {

    private final OpportunityAttachmentRepository attachmentRepository;
    private final StorageService storageService;

    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<Resource> download(@PathVariable UUID attachmentId) {
        OpportunityAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("첨부를 찾을 수 없습니다: " + attachmentId));
        if (attachment.getDownloadStatus() != AttachmentDownloadStatus.SUCCESS
                || attachment.getStorageUrl() == null) {
            throw new IllegalStateException("다운로드 가능한 첨부가 아닙니다(미수집): " + attachmentId);
        }
        byte[] bytes = storageService.load(attachment.getStorageUrl());
        String contentType = attachment.getContentType() != null
                ? attachment.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        String fileName = attachment.getFileName() != null ? attachment.getFileName() : "attachment";
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");

        log.info("[CR-019] 첨부 다운로드 (MCP): attachmentId={}, name={}", attachmentId, fileName);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .body(new ByteArrayResource(bytes));
    }
}
