package com.biddingagency.controller;

import com.biddingagency.domain.compliance.dto.RequiredDocumentSlot;
import com.biddingagency.domain.compliance.dto.RequiredDocumentSlotsResponse;
import com.biddingagency.domain.compliance.service.ComplianceService;
import com.biddingagency.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * 요구사항 슬롯 (CR-010)
 *
 * 공고 BLOCKER 요구사항을 슬롯으로 펼쳐 고객 서류와 1:1 매핑.
 * DOCS_PENDING → DOCS_RECEIVED 전이의 사전 조건(BIZ-015).
 */
@Slf4j
@RestController
@RequestMapping("/bid-requests/{bidRequestId}/required-document-slots")
@RequiredArgsConstructor
@Tag(name = "Required Document Slots", description = "공고 요구서류 슬롯 매칭 (CR-010)")
public class RequiredDocumentSlotController {

    private final ComplianceService complianceService;

    @GetMapping
    @Operation(summary = "요구사항 슬롯 목록")
    public ResponseEntity<RequiredDocumentSlotsResponse> getSlots(@PathVariable UUID bidRequestId) {
        return ResponseEntity.ok(complianceService.getRequiredDocumentSlots(bidRequestId));
    }

    @PostMapping(path = "/{requirementItemId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "슬롯에 직접 업로드")
    public ResponseEntity<RequiredDocumentSlot> uploadToSlot(
            @PathVariable UUID bidRequestId,
            @PathVariable UUID requirementItemId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        RequiredDocumentSlot slot = complianceService.uploadToSlot(
                bidRequestId, requirementItemId, file, userDetails.getMember());
        return ResponseEntity.ok(slot);
    }

    @DeleteMapping("/{requirementItemId}")
    @Operation(summary = "슬롯 매핑 해제")
    public ResponseEntity<Void> unmapSlot(
            @PathVariable UUID bidRequestId,
            @PathVariable UUID requirementItemId) {
        complianceService.unmapSlot(bidRequestId, requirementItemId);
        return ResponseEntity.noContent().build();
    }
}
