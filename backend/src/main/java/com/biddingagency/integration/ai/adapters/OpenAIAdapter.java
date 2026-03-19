package com.biddingagency.integration.ai.adapters;

import com.biddingagency.integration.ai.core.AIProvider;
import com.biddingagency.integration.ai.core.model.*;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * OpenAI Adapter
 *
 * OpenAI API를 AIProvider 인터페이스로 어댑팅
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.ai.providers.openai.enabled", havingValue = "true", matchIfMissing = true)
public class OpenAIAdapter implements AIProvider {

    private final OpenAiService openAiService;
    private final String defaultModel;
    private final double defaultTemperature;
    private final int defaultMaxTokens;

    public OpenAIAdapter(
            @Value("${app.ai.providers.openai.api-key:#{null}}") String apiKey,
            @Value("${app.ai.providers.openai.default-model:gpt-4}") String defaultModel,
            @Value("${app.ai.providers.openai.temperature:0.3}") double defaultTemperature,
            @Value("${app.ai.providers.openai.max-tokens:4000}") int defaultMaxTokens,
            @Value("${app.ai.providers.openai.timeout:300}") int timeoutSeconds) {

        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("OpenAI API key is not configured - provider will be unavailable");
            this.openAiService = null;
        } else {
            this.openAiService = new OpenAiService(apiKey, Duration.ofSeconds(timeoutSeconds));
            log.info("OpenAI Adapter initialized with model: {}", defaultModel);
        }

        this.defaultModel = defaultModel;
        this.defaultTemperature = defaultTemperature;
        this.defaultMaxTokens = defaultMaxTokens;
    }

    @Override
    public AIResponse chat(List<AIMessage> messages, AIRequestParams params) {
        if (!isAvailable()) {
            throw new IllegalStateException("OpenAI provider is not available (API key not configured)");
        }

        log.info("OpenAI chat request - model: {}, messages: {}",
                params.getModel() != null ? params.getModel() : defaultModel,
                messages.size());

        try {
            // AIMessage → ChatMessage 변환
            List<ChatMessage> chatMessages = messages.stream()
                    .map(this::convertMessage)
                    .collect(Collectors.toList());

            // 요청 빌드
            ChatCompletionRequest.ChatCompletionRequestBuilder requestBuilder = ChatCompletionRequest.builder()
                    .model(params.getModel() != null ? params.getModel() : defaultModel)
                    .messages(chatMessages)
                    .temperature(params.getTemperature() != null ? params.getTemperature() : defaultTemperature)
                    .maxTokens(params.getMaxTokens() != null ? params.getMaxTokens() : defaultMaxTokens);

            // 선택적 파라미터
            if (params.getTopP() != null) {
                requestBuilder.topP(params.getTopP());
            }
            if (params.getFrequencyPenalty() != null) {
                requestBuilder.frequencyPenalty(params.getFrequencyPenalty());
            }
            if (params.getPresencePenalty() != null) {
                requestBuilder.presencePenalty(params.getPresencePenalty());
            }

            ChatCompletionRequest request = requestBuilder.build();

            // API 호출
            ChatCompletionResult result = openAiService.createChatCompletion(request);

            // 응답 변환
            AIResponse response = convertResponse(result);

            log.info("OpenAI response received - tokens: {}", response.getTokenUsage().getTotalTokens());

            return response;

        } catch (Exception e) {
            log.error("OpenAI API call failed", e);
            throw new RuntimeException("OpenAI 요청 실패: " + e.getMessage(), e);
        }
    }

    @Override
    public AIResponse chatJson(List<AIMessage> messages, AIRequestParams params) {
        // JSON 모드 활성화
        AIRequestParams jsonParams = AIRequestParams.builder()
                .model(params.getModel())
                .temperature(0.2)  // JSON 모드는 낮은 temperature 권장
                .maxTokens(params.getMaxTokens())
                .jsonMode(true)
                .build();

        return chat(messages, jsonParams);
    }

    @Override
    public boolean supports(AIFeature feature) {
        return switch (feature) {
            case CHAT_COMPLETION -> true;
            case JSON_MODE -> true;
            case FUNCTION_CALLING -> true;
            case VISION -> true;
            case LONG_CONTEXT -> true; // GPT-4 Turbo는 128K 지원
            case STREAMING -> false; // MVP에서는 미지원
        };
    }

    @Override
    public boolean isAvailable() {
        return openAiService != null;
    }

    @Override
    public String getProviderId() {
        return "openai";
    }

    @Override
    public String getProviderDisplayName() {
        return "OpenAI " + defaultModel.toUpperCase();
    }

    @Override
    public String getDefaultModel() {
        return defaultModel;
    }

    /**
     * AIMessage → ChatMessage 변환
     */
    private ChatMessage convertMessage(AIMessage message) {
        String role = switch (message.getRole()) {
            case SYSTEM -> ChatMessageRole.SYSTEM.value();
            case USER -> ChatMessageRole.USER.value();
            case ASSISTANT -> ChatMessageRole.ASSISTANT.value();
        };

        return new ChatMessage(role, message.getContent());
    }

    /**
     * ChatCompletionResult → AIResponse 변환
     */
    private AIResponse convertResponse(ChatCompletionResult result) {
        String content = result.getChoices().get(0).getMessage().getContent();

        AIResponse.TokenUsage tokenUsage = AIResponse.TokenUsage.builder()
                .promptTokens(result.getUsage().getPromptTokens())
                .completionTokens(result.getUsage().getCompletionTokens())
                .totalTokens(result.getUsage().getTotalTokens())
                .build();

        return AIResponse.builder()
                .content(content)
                .model(result.getModel())
                .tokenUsage(tokenUsage)
                .providerId(getProviderId())
                .finishReason(result.getChoices().get(0).getFinishReason())
                .build();
    }
}
