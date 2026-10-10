package com.biddingagency.controller;

import com.biddingagency.domain.inquiry.service.InquiryService;
import com.biddingagency.dto.InquiryResponses;
import com.biddingagency.dto.InquirySubmitRequest;
import com.biddingagency.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 고객센터 1:1 문의 (CR-132). 비로그인도 쓸 수 있고, 로그인 상태면 회원과 묶는다.
 */
@RestController
@RequestMapping("/inquiries")
@RequiredArgsConstructor
@Tag(name = "Inquiries", description = "Customer support 1:1 inquiries")
public class InquiryController {

    private final InquiryService inquiryService;

    @GetMapping("/types")
    @Operation(summary = "List active inquiry types")
    public ResponseEntity<List<InquiryResponses.TypeItem>> types() {
        return ResponseEntity.ok(inquiryService.listTypes());
    }

    @PostMapping
    @Operation(summary = "Submit an inquiry (login optional)")
    public ResponseEntity<InquiryResponses.Submitted> submit(
            @Valid @RequestBody InquirySubmitRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inquiryService.submit(request, userDetails != null ? userDetails.getMember() : null));
    }
}
