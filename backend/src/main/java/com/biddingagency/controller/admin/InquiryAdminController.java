package com.biddingagency.controller.admin;

import com.biddingagency.domain.inquiry.entity.InquiryStatus;
import com.biddingagency.domain.inquiry.service.InquiryService;
import com.biddingagency.dto.InquiryAnswerRequest;
import com.biddingagency.dto.InquiryResponses;
import com.biddingagency.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/admin/inquiries")
@RequiredArgsConstructor
@Tag(name = "Admin - Inquiries", description = "Admin 1:1 inquiry list and answer")
@PreAuthorize("hasRole('ADMIN')")
public class InquiryAdminController {

    private final InquiryService inquiryService;

    @GetMapping
    @Operation(summary = "List inquiries")
    public ResponseEntity<Page<InquiryResponses.ListItem>> list(
            @RequestParam(required = false) InquiryStatus status,
            @RequestParam(required = false) String typeCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(inquiryService.search(status, typeCode, page, size));
    }

    @GetMapping("/counts")
    @Operation(summary = "Count inquiries by status")
    public ResponseEntity<InquiryResponses.Counts> counts() {
        return ResponseEntity.ok(inquiryService.counts());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get inquiry detail")
    public ResponseEntity<InquiryResponses.Detail> detail(@PathVariable UUID id) {
        return ResponseEntity.ok(inquiryService.detail(id));
    }

    @PostMapping("/{id}/answer")
    @Operation(summary = "Answer an inquiry by mail to the customer")
    public ResponseEntity<InquiryResponses.Detail> answer(
            @PathVariable UUID id,
            @Valid @RequestBody InquiryAnswerRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(inquiryService.answer(id, request.getAnswer(), userDetails.getMember()));
    }
}
