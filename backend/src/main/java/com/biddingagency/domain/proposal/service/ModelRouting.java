package com.biddingagency.domain.proposal.service;

import com.biddingagency.domain.proposal.entity.GenerationTargetType;

/**
 * CR-029 모델 라우팅 정책 + 단가 (비용 추적용).
 *
 * <p>현재 범위(2026-06-01 결정): <b>비용 추적 + 로깅만</b>. 모델을 Aimbase 워크플로우에 실제 강제하지 않는다.
 * 즉 여기 모델명은 "이 단계는 이 모델로 돈다고 약속된 값"이며, generation_log.model_name 과 cost 계산의 기준이다.
 * 실제 강제(input.model → step config override)는 Aimbase 워크플로우 수정이 필요해 후속 단계로 분리.
 *
 * <p>정본 라우팅(cr029-model-routing-cost):
 * <ul>
 *   <li>design → Haiku 4.5 (구조 발췌·검증)</li>
 *   <li>write-section → Sonnet 4.6 (본문 분량·깊이)</li>
 *   <li>assemble → Sonnet 4.6 (문체 통일)</li>
 * </ul>
 */
public final class ModelRouting {

    private ModelRouting() {}

    // 모델 ID (CLAUDE.md 기준)
    public static final String HAIKU  = "claude-haiku-4-5-20251001";
    public static final String SONNET = "claude-sonnet-4-6";

    /** 파이프라인 단계 → 라우팅 모델. */
    public static String modelFor(GenerationTargetType stage) {
        return switch (stage) {
            case DESIGN -> HAIKU;
            case WRITE_SECTION, ASSEMBLE -> SONNET;
            case VERIFY -> HAIKU;          // [[cr031]] 검증은 Haiku
            default -> SONNET;             // 알 수 없는 단계는 보수적으로 Sonnet 단가
        };
    }

    // ── 단가 (USD per 1M tokens) ──────────────────────────────────────────
    // 출처: Anthropic 공개 단가. 단가 변동·할인 미반영 — 운영 청구서와 대조해 보정 필요(미검증 값).
    // Haiku 4.5: input $1.00 / output $5.00
    // Sonnet 4.6: input $3.00 / output $15.00
    private static final double HAIKU_IN  = 1.00,  HAIKU_OUT  = 5.00;
    private static final double SONNET_IN = 3.00,  SONNET_OUT = 15.00;

    public static double inputPricePerMTok(String model) {
        return HAIKU.equals(model) ? HAIKU_IN : SONNET_IN;
    }

    public static double outputPricePerMTok(String model) {
        return HAIKU.equals(model) ? HAIKU_OUT : SONNET_OUT;
    }
}
