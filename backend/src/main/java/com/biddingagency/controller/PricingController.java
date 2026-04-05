package com.biddingagency.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/pricing")
@Tag(name = "Pricing", description = "서비스 요금 안내")
public class PricingController {

    @GetMapping
    @Operation(summary = "서비스 레벨별 요금 정보")
    public ResponseEntity<List<Map<String, Object>>> getPricing() {
        return ResponseEntity.ok(List.of(
                Map.of(
                        "id", "basic",
                        "name", "Basic",
                        "price", 299,
                        "currency", "USD",
                        "period", "per bid",
                        "features", List.of(
                                "SAM.gov 공고 모니터링",
                                "기본 입찰 문서 생성",
                                "이메일 알림",
                                "1건 동시 입찰"
                        ),
                        "recommended", false
                ),
                Map.of(
                        "id", "professional",
                        "name", "Professional",
                        "price", 599,
                        "currency", "USD",
                        "period", "per bid",
                        "features", List.of(
                                "Basic 포함 전체 기능",
                                "AI 기반 문서 최적화",
                                "컴플라이언스 자동 검증",
                                "5건 동시 입찰",
                                "우선 지원"
                        ),
                        "recommended", true
                ),
                Map.of(
                        "id", "enterprise",
                        "name", "Enterprise",
                        "price", 999,
                        "currency", "USD",
                        "period", "per bid",
                        "features", List.of(
                                "Professional 포함 전체 기능",
                                "전담 매니저 배정",
                                "맞춤형 문서 템플릿",
                                "무제한 동시 입찰",
                                "API 연동 지원",
                                "24/7 긴급 지원"
                        ),
                        "recommended", false
                )
        ));
    }
}
