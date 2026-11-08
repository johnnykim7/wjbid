package com.biddingagency.controller.admin;

import com.biddingagency.domain.bid.service.BidPriceService;
import com.biddingagency.domain.compliance.entity.SubmissionDisposition;
import com.biddingagency.domain.compliance.service.SubmissionCheckService;
import com.biddingagency.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/admin/bid-requests/{id}")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Submission Check", description = "제출 검사와 가격 표 (CR-194)")
public class SubmissionCheckAdminController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final SubmissionCheckService submissionCheckService;
    private final BidPriceService bidPriceService;

    @GetMapping("/submission-check")
    @Operation(summary = "제출 검사 결과", description = "산출물을 바꾸지 않고 입찰 상태 전이도 막지 않는다")
    public ResponseEntity<SubmissionCheckService.Check> check(@PathVariable UUID id) {
        return ResponseEntity.ok(submissionCheckService.check(id));
    }

    @PutMapping("/submission-check/lines/{reqId}")
    @Operation(summary = "요구 줄 처리", description = "NOT_ANSWERED 는 reason, sourceRef 필수, EQUIVALENT 는 reason 필수")
    public ResponseEntity<SubmissionCheckService.Check> saveLine(@PathVariable UUID id, @PathVariable String reqId,
                                                                 @Valid @RequestBody LineNoteRequest request,
                                                                 @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(submissionCheckService.saveLineNote(id, reqId, request.disposition(),
                request.reason(), request.sourceRef(), userDetails.getUsername()));
    }

    @DeleteMapping("/submission-check/lines/{reqId}")
    @Operation(summary = "요구 줄 처리 지우기")
    public ResponseEntity<SubmissionCheckService.Check> clearLine(@PathVariable UUID id, @PathVariable String reqId) {
        return ResponseEntity.ok(submissionCheckService.clearLineNote(id, reqId));
    }

    @PutMapping("/submission-check/conditions/{key}")
    @Operation(summary = "결정 조건 해소 근거 적기")
    public ResponseEntity<SubmissionCheckService.Check> resolveCondition(@PathVariable UUID id, @PathVariable String key,
                                                                         @Valid @RequestBody ConditionNoteRequest request,
                                                                         @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(submissionCheckService.resolveCondition(id, key, request.reason(), userDetails.getUsername()));
    }

    @DeleteMapping("/submission-check/conditions/{key}")
    @Operation(summary = "결정 조건 해소 지우기")
    public ResponseEntity<SubmissionCheckService.Check> clearCondition(@PathVariable UUID id, @PathVariable String key) {
        return ResponseEntity.ok(submissionCheckService.clearCondition(id, key));
    }

    @GetMapping("/price-table")
    @Operation(summary = "가격 표 조회")
    public ResponseEntity<BidPriceService.PriceTable> priceTable(@PathVariable UUID id) {
        return ResponseEntity.ok(bidPriceService.adminTable(id));
    }

    @GetMapping("/price-table/export")
    @Operation(summary = "가격 표 엑셀 내려받기")
    public ResponseEntity<byte[]> exportPriceTable(@PathVariable UUID id) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"price-table.xlsx\"")
                .contentType(XLSX)
                .body(bidPriceService.adminExport(id));
    }

    public record LineNoteRequest(@NotNull SubmissionDisposition disposition, @Size(max = 4000) String reason,
                                  @Size(max = 500) String sourceRef) {
    }

    public record ConditionNoteRequest(@Size(max = 4000) String reason) {
    }
}
