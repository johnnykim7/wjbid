package com.biddingagency.integration.ai.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI Request Parameters
 *
 * AI 요청 파라미터
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIRequestParams {

    /**
     * 모델 이름 (예: gpt-4, claude-3-5-sonnet-20240620)
     */
    private String model;

    /**
     * Temperature (0.0~1.0, 낮을수록 결정적)
     */
    @Builder.Default
    private Double temperature = 0.3;

    /**
     * 최대 토큰 수
     */
    @Builder.Default
    private Integer maxTokens = 4000;

    /**
     * JSON 모드 사용 여부
     */
    @Builder.Default
    private Boolean jsonMode = false;

    /**
     * Top P (확률 샘플링)
     */
    private Double topP;

    /**
     * Frequency penalty
     */
    private Double frequencyPenalty;

    /**
     * Presence penalty
     */
    private Double presencePenalty;
}
