package com.biddingagency.integration.ai.core.model;

/**
 * AI Feature Enum
 *
 * AI 제공자가 지원하는 기능
 */
public enum AIFeature {
    /**
     * 기본 채팅 완성
     */
    CHAT_COMPLETION,

    /**
     * JSON 모드 (structured output)
     */
    JSON_MODE,

    /**
     * 함수 호출 (function calling)
     */
    FUNCTION_CALLING,

    /**
     * 비전 (이미지 이해)
     */
    VISION,

    /**
     * 긴 문맥 처리 (128K+ 토큰)
     */
    LONG_CONTEXT,

    /**
     * 스트리밍 응답
     */
    STREAMING
}
