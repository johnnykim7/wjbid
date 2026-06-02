package com.biddingagency.controller;

import com.biddingagency.domain.document.dto.BidDocumentDto;
import com.biddingagency.domain.document.dto.BidDocumentVersionDto;
import com.biddingagency.domain.document.dto.SaveVersionRequest;
import com.biddingagency.domain.document.entity.BidDocument;
import com.biddingagency.domain.document.entity.BidDocumentVersion;
import com.biddingagency.domain.document.service.BidDocumentService;
import com.biddingagency.domain.document.service.DocumentVersionService;
import com.biddingagency.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/bid-documents")
@RequiredArgsConstructor
@Tag(name = "Bid Documents", description = "문서 편집 관리")
public class BidDocumentController {

    private final BidDocumentService bidDocumentService;
    private final DocumentVersionService documentVersionService;

    @GetMapping
    @Operation(summary = "입찰 요청별 문서 목록")
    public ResponseEntity<List<BidDocumentDto>> listByBidRequest(@RequestParam UUID bidRequestId) {
        List<BidDocumentDto> docs = bidDocumentService.findByBidRequest(bidRequestId)
                .stream()
                .map(BidDocumentDto::from)
                .toList();
        return ResponseEntity.ok(docs);
    }

    @GetMapping("/{id}")
    @Operation(summary = "문서 상세 (현재 버전 포함)")
    public ResponseEntity<BidDocumentDto> getDocument(@PathVariable UUID id) {
        BidDocument doc = bidDocumentService.findById(id);
        BidDocumentVersion latestVersion = documentVersionService.getLatestVersion(id);
        return ResponseEntity.ok(BidDocumentDto.withVersion(doc, latestVersion));
    }

    @GetMapping("/{id}/versions")
    @Operation(summary = "버전 목록")
    public ResponseEntity<List<BidDocumentVersionDto>> getVersions(@PathVariable UUID id) {
        bidDocumentService.findById(id); // existence check
        List<BidDocumentVersionDto> versions = documentVersionService.getAllVersions(id)
                .stream()
                .map(BidDocumentVersionDto::summary)
                .toList();
        return ResponseEntity.ok(versions);
    }

    @GetMapping("/{id}/versions/{versionNo}")
    @Operation(summary = "특정 버전 조회")
    public ResponseEntity<BidDocumentVersionDto> getVersion(
            @PathVariable UUID id,
            @PathVariable int versionNo) {
        bidDocumentService.findById(id); // existence check
        BidDocumentVersion version = documentVersionService.getVersion(id, versionNo);
        return ResponseEntity.ok(BidDocumentVersionDto.from(version));
    }

    @PostMapping("/{id}/versions")
    @Operation(summary = "새 버전 저장 (편집)")
    public ResponseEntity<BidDocumentVersionDto> saveVersion(
            @PathVariable UUID id,
            @Valid @RequestBody SaveVersionRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        BidDocumentVersion version = documentVersionService.saveVersion(
                id,
                request.getContentJson(),
                request.getChangeSummary(),
                userDetails.getMember().getId()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BidDocumentVersionDto.from(version));
    }

    @PostMapping("/{id}/lock")
    @Operation(summary = "문서 잠금 (DRAFT → LOCKED)")
    public ResponseEntity<BidDocumentDto> lockDocument(@PathVariable UUID id) {
        documentVersionService.lockDocument(id);
        BidDocument doc = bidDocumentService.findById(id);
        return ResponseEntity.ok(BidDocumentDto.from(doc));
    }

    @PostMapping("/{id}/unlock")
    @Operation(summary = "잠금 해제 (LOCKED → DRAFT)")
    public ResponseEntity<BidDocumentDto> unlockDocument(@PathVariable UUID id) {
        documentVersionService.unlockDocument(id);
        BidDocument doc = bidDocumentService.findById(id);
        return ResponseEntity.ok(BidDocumentDto.from(doc));
    }

    @GetMapping("/{id}/export/pdf")
    @Operation(summary = "PDF 내보내기")
    public ResponseEntity<byte[]> exportPdf(@PathVariable UUID id) {
        BidDocument doc = bidDocumentService.findById(id);
        BidDocumentVersion latestVersion = documentVersionService.getLatestVersion(id);

        byte[] pdfBytes = generatePdf(doc, latestVersion);

        String filename = doc.getDocumentType().name().toLowerCase() + "_v" + latestVersion.getVersionNo() + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdfBytes.length)
                .body(pdfBytes);
    }

    private byte[] generatePdf(BidDocument doc, BidDocumentVersion version) {
        try {
            var baos = new java.io.ByteArrayOutputStream();
            var writer = new com.itextpdf.kernel.pdf.PdfWriter(baos);
            var pdfDoc = new com.itextpdf.kernel.pdf.PdfDocument(writer);
            var document = new com.itextpdf.layout.Document(pdfDoc);

            // Title
            document.add(new com.itextpdf.layout.element.Paragraph(
                    doc.getDocumentType().name())
                    .setFontSize(18)
                    .setBold());

            // Version info
            document.add(new com.itextpdf.layout.element.Paragraph(
                    "Version: " + version.getVersionLabel() + " | Edited: " + version.getEditedAt())
                    .setFontSize(10));

            document.add(new com.itextpdf.layout.element.Paragraph("\n"));

            // Content from JSON
            Map<String, Object> contentJson = version.getContentJson();
            String textContent = extractTextFromTipTapJson(contentJson);
            document.add(new com.itextpdf.layout.element.Paragraph(textContent).setFontSize(12));

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate PDF for document {}", doc.getId(), e);
            throw new RuntimeException("PDF generation failed", e);
        }
    }

    @SuppressWarnings("unchecked")
    private String extractTextFromTipTapJson(Map<String, Object> json) {
        StringBuilder sb = new StringBuilder();
        if (json == null) return "";

        Object content = json.get("content");
        if (content instanceof List<?> nodes) {
            for (Object node : nodes) {
                if (node instanceof Map<?, ?> nodeMap) {
                    extractTextRecursive((Map<String, Object>) nodeMap, sb);
                    sb.append("\n");
                }
            }
        }

        if (sb.isEmpty()) {
            return json.toString();
        }
        return sb.toString().trim();
    }

    @SuppressWarnings("unchecked")
    private void extractTextRecursive(Map<String, Object> node, StringBuilder sb) {
        if ("text".equals(node.get("type"))) {
            Object text = node.get("text");
            if (text != null) sb.append(text);
            return;
        }

        Object content = node.get("content");
        if (content instanceof List<?> children) {
            for (Object child : children) {
                if (child instanceof Map<?, ?> childMap) {
                    extractTextRecursive((Map<String, Object>) childMap, sb);
                }
            }
        }
    }
}
