package com.biddingagency.controller;

import com.biddingagency.domain.bid.entity.ClientDocument;
import com.biddingagency.domain.bid.repository.ClientDocumentRepository;
import com.biddingagency.domain.bid.service.BidRequestService;
import com.biddingagency.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/bid-requests/{bidRequestId}/client-documents")
@RequiredArgsConstructor
@Tag(name = "Client Documents", description = "고객 문서 업로드 관리")
public class ClientDocumentController {

    private final ClientDocumentRepository clientDocumentRepository;
    private final BidRequestService bidRequestService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "고객 문서 업로드")
    public ResponseEntity<ClientDocument> upload(
            @PathVariable UUID bidRequestId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", required = false) String category,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        var bidRequest = bidRequestService.findById(bidRequestId);

        // BIZ-011: file size limit 50MB
        if (file.getSize() > 50 * 1024 * 1024) {
            throw new IllegalArgumentException("File size exceeds 50MB limit");
        }

        // TODO: MinIO 업로드 구현 — 현재는 storage_url placeholder
        String storageUrl = "minio://bidding-agency/client-docs/" + bidRequestId + "/" + file.getOriginalFilename();

        ClientDocument doc = ClientDocument.builder()
                .bidRequest(bidRequest)
                .member(userDetails.getMember())
                .fileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .storageUrl(storageUrl)
                .documentCategory(category)
                .build();

        ClientDocument saved = clientDocumentRepository.save(doc);
        log.info("Client document uploaded: {} for bid request {}", saved.getId(), bidRequestId);

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    @Operation(summary = "제출 문서 목록")
    public ResponseEntity<List<ClientDocument>> list(@PathVariable UUID bidRequestId) {
        bidRequestService.findById(bidRequestId); // existence check
        return ResponseEntity.ok(clientDocumentRepository.findByBidRequestId(bidRequestId));
    }

    @DeleteMapping("/{docId}")
    @Operation(summary = "제출 문서 삭제")
    public ResponseEntity<Void> delete(
            @PathVariable UUID bidRequestId,
            @PathVariable UUID docId) {
        bidRequestService.findById(bidRequestId); // existence check
        clientDocumentRepository.deleteById(docId);
        log.info("Client document deleted: {} from bid request {}", docId, bidRequestId);
        return ResponseEntity.noContent().build();
    }
}
