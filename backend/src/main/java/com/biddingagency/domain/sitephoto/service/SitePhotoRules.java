package com.biddingagency.domain.sitephoto.service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 고객 화면 사진 올리기 규칙 (CR-153, POL-039)
 *
 * @param allowedTypes  받는 형식(소문자 MIME)
 * @param maxUploadMb   한 장 무게 상한(MB)
 * @param maxMegapixels 원본 화소 수 상한(백만 화소)
 * @param minWidth      가로 최소
 * @param saveWidth     저장 가로
 * @param saveMaxKb     저장 무게 목표(KB)
 */
public record SitePhotoRules(List<String> allowedTypes, long maxUploadMb, long maxMegapixels,
                             int minWidth, int saveWidth, int saveMaxKb) {

    /** 안내 문구에 쓰는 형식 이름. image/jpeg,image/png 이면 "JPEG, PNG" */
    public String allowedTypesLabel() {
        return allowedTypes.stream()
                .map(t -> t.substring(t.indexOf('/') + 1).toUpperCase())
                .collect(Collectors.joining(", "));
    }

    public long maxUploadBytes() {
        return maxUploadMb * 1024L * 1024L;
    }

    public long maxPixels() {
        return maxMegapixels * 1_000_000L;
    }

    public long saveMaxBytes() {
        return saveMaxKb * 1024L;
    }
}
