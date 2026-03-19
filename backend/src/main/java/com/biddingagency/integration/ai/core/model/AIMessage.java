package com.biddingagency.integration.ai.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI Message
 *
 * 채팅 메시지 (사용자/시스템/AI 응답)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIMessage {

    public enum Role {
        SYSTEM,    // 시스템 프롬프트 (AI 역할 정의)
        USER,      // 사용자 메시지
        ASSISTANT  // AI 응답
    }

    /**
     * 메시지 역할
     */
    private Role role;

    /**
     * 메시지 내용
     */
    private String content;

    /**
     * System 메시지 생성
     */
    public static AIMessage system(String content) {
        return AIMessage.builder()
                .role(Role.SYSTEM)
                .content(content)
                .build();
    }

    /**
     * User 메시지 생성
     */
    public static AIMessage user(String content) {
        return AIMessage.builder()
                .role(Role.USER)
                .content(content)
                .build();
    }

    /**
     * Assistant 메시지 생성
     */
    public static AIMessage assistant(String content) {
        return AIMessage.builder()
                .role(Role.ASSISTANT)
                .content(content)
                .build();
    }
}
