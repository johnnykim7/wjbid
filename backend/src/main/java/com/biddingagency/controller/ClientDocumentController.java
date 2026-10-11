package com.biddingagency.controller;

import com.biddingagency.domain.bid.entity.ClientDocument;
import com.biddingagency.domain.bid.service.ClientDocumentService;
import com.biddingagency.security.CustomUserDetails;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/bid-requests/{bidRequestId}/client-documents")
@RequiredArgsConstructor
@Tag(name = "Client Documents", description = "고객 문서 업로드 관리")
public class ClientDocumentController {

    private final ClientDocumentService clientDocumentService;

    /** CR-024: 엔티티 직렬화 시 Member/BidRequest LAZY 폭발 방지용 가벼운 DTO */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ClientDocumentDto(
            String id,
            String fileName,
            Long fileSize,
            String contentType,
            String documentCategory,
            String storageUrl,
            LocalDateTime createdAt
    ) {
        public static ClientDocumentDto from(ClientDocument d) {
            return new ClientDocumentDto(
                    d.getId() != null ? d.getId().toString() : null,
                    d.getFileName(),
                    d.getFileSize(),
                    d.getContentType(),
                    d.getDocumentCategory(),
                    d.getStorageUrl(),
                    d.getCreatedAt()
            );
        }
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "고객 문서 업로드")
    public ResponseEntity<ClientDocumentDto> upload(
            @PathVariable UUID bidRequestId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", required = false) String category,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ClientDocument saved = clientDocumentService.upload(bidRequestId, userDetails.getMember(), file, category);
        return ResponseEntity.status(HttpStatus.CREATED).body(ClientDocumentDto.from(saved));
    }

    @GetMapping
    @Operation(summary = "제출 문서 목록")
    public ResponseEntity<List<ClientDocumentDto>> list(
            @PathVariable UUID bidRequestId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<ClientDocumentDto> dtos = clientDocumentService.list(bidRequestId, userDetails.getMember())
                .stream().map(ClientDocumentDto::from).toList();
        return ResponseEntity.ok(dtos);
    }

    @DeleteMapping("/{docId}")
    @Operation(summary = "제출 문서 삭제")
    public ResponseEntity<Void> delete(
            @PathVariable UUID bidRequestId,
            @PathVariable UUID docId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        clientDocumentService.delete(bidRequestId, docId, userDetails.getMember());
        return ResponseEntity.noContent().build();
    }
}
