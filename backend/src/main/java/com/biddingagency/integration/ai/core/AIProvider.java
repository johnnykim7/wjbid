package com.biddingagency.integration.ai.core;

import com.biddingagency.integration.ai.core.model.*;

import java.util.List;

/**
 * AI Provider Interface
 *
 * 모든 AI 제공자가 구현해야 하는 인터페이스
 * (Adapter Pattern의 Target Interface)
 */
public interface AIProvider {

    /**
     * 채팅 완성
     *
     * @param messages 대화 메시지 리스트
     * @param params 요청 파라미터
     * @return AI 응답
     */
    AIResponse chat(List<AIMessage> messages, AIRequestParams params);

    /**
     * 채팅 완성 (간편 버전 - system + user)
     *
     * @param systemPrompt 시스템 프롬프트
     * @param userPrompt 사용자 프롬프트
     * @return AI 응답
     */
    default AIResponse chat(String systemPrompt, String userPrompt) {
        List<AIMessage> messages = List.of(
                AIMessage.system(systemPrompt),
                AIMessage.user(userPrompt)
        );
        return chat(messages, AIRequestParams.builder().build());
    }

    /**
     * JSON 모드 채팅 (structured output)
     *
     * @param messages 대화 메시지 리스트
     * @param params 요청 파라미터
     * @return AI 응답 (JSON 형식)
     */
    AIResponse chatJson(List<AIMessage> messages, AIRequestParams params);

    /**
     * JSON 모드 채팅 (간편 버전)
     *
     * @param systemPrompt 시스템 프롬프트
     * @param userPrompt 사용자 프롬프트
     * @return AI 응답 (JSON 형식)
     */
    default AIResponse chatJson(String systemPrompt, String userPrompt) {
        List<AIMessage> messages = List.of(
                AIMessage.system(systemPrompt +
                        "\n\nIMPORTANT: You must respond in valid JSON format only."),
                AIMessage.user(userPrompt)
        );
        AIRequestParams params = AIRequestParams.builder()
                .jsonMode(true)
                .temperature(0.2)  // JSON 모드는 낮은 temperature 권장
                .build();
        return chatJson(messages, params);
    }

    /**
     * 기능 지원 여부 확인
     *
     * @param feature 확인할 기능
     * @return 지원 여부
     */
    boolean supports(AIFeature feature);

    /**
     * 가용성 확인
     *
     * @return 현재 사용 가능 여부
     */
    boolean isAvailable();

    /**
     * 제공자 ID 반환
     *
     * @return 제공자 ID (예: "openai", "claude", "gemini")
     */
    String getProviderId();

    /**
     * 제공자 표시 이름 반환
     *
     * @return 표시 이름 (예: "OpenAI GPT-4")
     */
    String getProviderDisplayName();

    /**
     * 기본 모델 반환
     *
     * @return 기본 모델 이름
     */
    String getDefaultModel();
}
