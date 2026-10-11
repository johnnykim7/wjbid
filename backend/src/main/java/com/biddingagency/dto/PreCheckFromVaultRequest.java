package com.biddingagency.dto;

import java.util.UUID;

/**
 * 공통 서류함 서류를 요구 항목에 잇는 요청 (CR-141, BID-MKT-023)
 */
public record PreCheckFromVaultRequest(UUID memberDocumentId) {
}
