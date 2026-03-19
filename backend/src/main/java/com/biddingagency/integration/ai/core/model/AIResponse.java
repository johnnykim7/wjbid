package com.biddingagency.integration.ai.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI Response
 *
 * AI 응답
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIResponse {

    /**
     * 응답 내용
     */
    private String content;

    /**
     * 사용된 모델
     */
    private String model;

    /**
     * 사용된 토큰 수
     */
    private TokenUsage tokenUsage;

    /**
     * 제공자 ID (openai, claude, gemini 등)
     */
    private String providerId;

    /**
     * 완료 이유 (stop, length, content_filter 등)
     */
    private String finishReason;

    /**
     * 토큰 사용량
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TokenUsage {
        private long promptTokens;
        private long completionTokens;
        private long totalTokens;
    }
}
