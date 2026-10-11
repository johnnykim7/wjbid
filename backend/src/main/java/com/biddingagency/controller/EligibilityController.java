package com.biddingagency.controller;

import com.biddingagency.domain.eligibility.service.EligibilityService;
import com.biddingagency.dto.EligibilityAnswerRequest;
import com.biddingagency.dto.EligibilityResponses;
import com.biddingagency.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/eligibility/notices")
@RequiredArgsConstructor
@Tag(name = "Eligibility", description = "Customer eligibility self-check (CR-137)")
public class EligibilityController {

    private final EligibilityService eligibilityService;

    @GetMapping
    @Operation(summary = "List notices that can be checked")
    public ResponseEntity<List<EligibilityResponses.NoticeRow>> list(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(eligibilityService.listNotices(userDetails.getMember().getId()));
    }

    @GetMapping("/{noticeId}")
    @Operation(summary = "Eligibility items of one notice with my answers")
    public ResponseEntity<EligibilityResponses.Detail> detail(
            @PathVariable UUID noticeId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(eligibilityService.detail(userDetails.getMember().getId(), noticeId));
    }

    @PutMapping("/{noticeId}/answers/{itemNo}")
    @Operation(summary = "Save my answer to one eligibility item")
    public ResponseEntity<EligibilityResponses.Saved> answer(
            @PathVariable UUID noticeId,
            @PathVariable int itemNo,
            @RequestBody EligibilityAnswerRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(eligibilityService.answer(
                userDetails.getMember().getId(), noticeId, itemNo, request.answer()));
    }
}
