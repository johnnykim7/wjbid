package com.biddingagency.controller;

import com.biddingagency.domain.compliance.dto.AutoFillResult;
import com.biddingagency.domain.compliance.service.PreCheckService;
import com.biddingagency.dto.PreCheckFromVaultRequest;
import com.biddingagency.dto.PreCheckResponses;
import com.biddingagency.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * 요구사항 슬롯 (CR-010)
 *
 * 슬롯은 공고 분석의 제출 서류에서 채운 요구 항목이다(CR-141, BIZ-045).
 * 모든 동작은 그 입찰 건을 낸 회원만 한다(BIZ-049). 슬롯 목록은 GET /bid-requests/{id}/pre-check 가 대신한다.
 */
@RestController
@RequestMapping("/bid-requests/{bidRequestId}/required-document-slots")
@RequiredArgsConstructor
@Tag(name = "Required Document Slots", description = "공고 요구서류 슬롯 매칭 (CR-010, CR-141)")
public class RequiredDocumentSlotController {

    private final PreCheckService preCheckService;

    @PostMapping(path = "/{requirementItemId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "슬롯에 직접 업로드")
    public ResponseEntity<PreCheckResponses.ClientItem> uploadToSlot(
            @PathVariable UUID bidRequestId,
            @PathVariable UUID requirementItemId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(preCheckService.upload(
                userDetails.getMember(), bidRequestId, requirementItemId, file));
    }

    @PostMapping("/{requirementItemId}/from-vault")
    @Operation(summary = "공통 서류함 서류를 복사해 슬롯에 잇기 (CR-141)")
    public ResponseEntity<PreCheckResponses.ClientItem> fromVault(
            @PathVariable UUID bidRequestId,
            @PathVariable UUID requirementItemId,
            @RequestBody PreCheckFromVaultRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(preCheckService.fromVault(
                userDetails.getMember(), bidRequestId, requirementItemId, request.memberDocumentId()));
    }

    @DeleteMapping("/{requirementItemId}")
    @Operation(summary = "슬롯 매핑 해제")
    public ResponseEntity<Void> unmapSlot(
            @PathVariable UUID bidRequestId,
            @PathVariable UUID requirementItemId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        preCheckService.unlink(userDetails.getMember().getId(), bidRequestId, requirementItemId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/auto-fill")
    @Operation(summary = "서류함에서 미충족 슬롯 일괄 자동채움 (CR-125)")
    public ResponseEntity<AutoFillResult> autoFill(
            @PathVariable UUID bidRequestId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(preCheckService.autoFill(userDetails.getMember(), bidRequestId));
    }
}
