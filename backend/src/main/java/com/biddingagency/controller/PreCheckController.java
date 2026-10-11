package com.biddingagency.controller;

import com.biddingagency.domain.compliance.service.PreCheckService;
import com.biddingagency.dto.PreCheckResponses;
import com.biddingagency.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/bid-requests")
@RequiredArgsConstructor
@Tag(name = "Pre-check", description = "Customer pre-submission document check (CR-141)")
public class PreCheckController {

    private final PreCheckService preCheckService;

    @GetMapping("/my/pre-check")
    @Operation(summary = "Pre-check summary of my bid requests, nearest deadline first")
    public ResponseEntity<List<PreCheckResponses.Row>> listMine(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(preCheckService.listMine(userDetails.getMember().getId()));
    }

    @GetMapping("/{bidRequestId}/pre-check")
    @Operation(summary = "Pre-check of one bid request (owner only, otherwise 404)")
    public ResponseEntity<PreCheckResponses.Detail> detail(
            @PathVariable UUID bidRequestId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(preCheckService.detail(userDetails.getMember().getId(), bidRequestId));
    }
}
