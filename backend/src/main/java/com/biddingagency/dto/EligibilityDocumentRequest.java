package com.biddingagency.dto;

import java.util.UUID;

/**
 * CR-140 증빙 서류 붙이기. documentId 가 null 이면 뗀다.
 */
public record EligibilityDocumentRequest(UUID documentId) {
}
