package com.biddingagency.controller;

import com.biddingagency.domain.bid.service.BidPriceService;
import com.biddingagency.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
@RequestMapping("/bid-requests/{id}/price-table")
@RequiredArgsConstructor
@Tag(name = "Bid Price Table", description = "가격 표 (CR-194)")
public class BidPriceController {

    private final BidPriceService bidPriceService;

    @GetMapping
    @Operation(summary = "가격 표 조회")
    public ResponseEntity<BidPriceService.PriceTable> get(@PathVariable UUID id,
                                                          @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(bidPriceService.customerTable(id, userDetails.getMember()));
    }

    @PutMapping
    @Operation(summary = "단가 저장", description = "금액은 받지 않고 서버가 계산한다. unitPrice 가 null 이면 그 줄의 단가를 지운다")
    public ResponseEntity<BidPriceService.PriceTable> save(@PathVariable UUID id,
                                                           @Valid @RequestBody SaveRequest request,
                                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(bidPriceService.save(id, userDetails.getMember(), userDetails.getUsername(), request.unitPrices()));
    }

    @GetMapping("/export")
    @Operation(summary = "가격 표 엑셀 내려받기")
    public ResponseEntity<byte[]> export(@PathVariable UUID id,
                                         @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"price-table.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bidPriceService.customerExport(id, userDetails.getMember()));
    }

    public record SaveRequest(@NotNull List<BidPriceService.UnitPrice> unitPrices) {
    }
}
