package com.biddingagency.domain.proposal.service;

import java.util.Map;

/**
 * CR-029 모델 단가표 + cost 계산.
 *
 * <p><b>실측 기반 변경(2026-06-02)</b>: 초기 설계는 단계→모델을 정책값(design=Haiku)으로 추정했으나,
 * 실측 결과 proposal-design/write-section/assemble 3개 워크플로우가 전부 AGENT_CALL + 동일 connection
 * {@code bidding-claude-sonnet}(=claude-sonnet-4-20250514)을 사용 — design 도 실제로는 Sonnet 으로 돈다.
 * 따라서 모델은 추정하지 않고 {@code LLMPlatformClient.resolveStageModel()} 이 워크플로우→connection 에서
 * 실측 조회한다. 이 클래스는 그 모델명에 대한 단가표만 담당.
 *
 * <p>모델별 미등록 시 보수적으로 Sonnet 단가로 폴백.
 */
public final class ModelRouting {

    private ModelRouting() {}

    // ── 단가 (USD per 1M tokens). 출처: Anthropic 공개 단가. 운영 청구서 대조로 보정 필요(미검증 값). ──
    // [input, output]
    private static final double[] SONNET = {3.00, 15.00};   // Sonnet 4.x
    private static final double[] HAIKU  = {1.00,  5.00};   // Haiku 4.5
    private static final double[] OPUS   = {15.00, 75.00};  // Opus 4.x

    /** model id(=connection 의 config.model) prefix 로 단가 매핑. 알 수 없으면 Sonnet 단가. */
    private static double[] priceOf(String model) {
        if (model == null) return SONNET;
        String m = model.toLowerCase();
        if (m.contains("haiku")) return HAIKU;
        if (m.contains("opus"))  return OPUS;
        return SONNET; // sonnet 및 미상
    }

    public static double inputPricePerMTok(String model)  { return priceOf(model)[0]; }
    public static double outputPricePerMTok(String model) { return priceOf(model)[1]; }

    /** 토큰 매핑이 비어있을 때 cost 계산 헬퍼가 참조하는 키. */
    public static final String KEY_INPUT  = "input_tokens";
    public static final String KEY_OUTPUT = "output_tokens";

    static Map<String, double[]> table() {
        return Map.of("sonnet", SONNET, "haiku", HAIKU, "opus", OPUS);
    }
}
