package com.biddingagency.integration.llmplatform.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Aimbase API 공통 응답 래퍼 (CR-002)
 * Aimbase는 모든 응답을 { success: true/false, data: {...}, error: "..." } 구조로 반환.
 */
@Data
@NoArgsConstructor
public class AimbaseApiResponse<T> {
    private boolean success;
    private T data;
    private String error;
}
