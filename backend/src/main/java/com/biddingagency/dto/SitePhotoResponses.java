package com.biddingagency.dto;

import com.biddingagency.domain.sitephoto.service.SitePhotoRules;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 고객 화면 사진 응답 (CR-153)
 */
public final class SitePhotoResponses {

    private SitePhotoResponses() {
    }

    /** 관리자 사진 카드. originalWidth, originalHeight 는 함께 배포된 사진이면 비어 있다 */
    public record AdminPhoto(UUID id, String slotKey, String url, String fileName, int width, int height,
                             Integer originalWidth, Integer originalHeight, long sizeBytes, boolean bundled,
                             boolean visible, LocalDateTime createdAt) {
    }

    /** 관리자 사진 자리. photos 는 숨긴 사진도 담는다 */
    public record AdminSlot(String key, String name, String location, List<AdminPhoto> photos) {
    }

    public record AdminView(SitePhotoRules rules, List<AdminSlot> slots) {
    }

    /** 고객 화면용. 자리 키마다 보이는 사진 주소. 사진이 없는 자리는 빈 배열 */
    public record PublicView(Map<String, List<String>> slots) {
    }

    public record VisibleRequest(Boolean visible) {
    }
}
