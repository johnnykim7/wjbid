package com.biddingagency.controller;

import com.biddingagency.domain.pricing.service.PricingService;
import com.biddingagency.dto.PricingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pricing")
@RequiredArgsConstructor
@Tag(name = "Pricing", description = "서비스 요금 안내")
public class PricingController {

    private final PricingService pricingService;

    @GetMapping
    @Operation(summary = "회원 월 금액 조회")
    public ResponseEntity<PricingResponse> getPricing() {
        return ResponseEntity.ok(pricingService.getPricing());
    }
}
