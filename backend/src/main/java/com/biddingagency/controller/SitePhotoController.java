package com.biddingagency.controller;

import com.biddingagency.domain.sitephoto.service.SitePhotoService;
import com.biddingagency.dto.SitePhotoResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.UUID;

/**
 * 고객 화면 사진 (CR-153, BID-SITE-002). 로그인 없이 읽는다
 */
@RestController
@RequestMapping("/site-photos")
@RequiredArgsConstructor
@Tag(name = "Site photos", description = "Photos shown in customer screen slots")
public class SitePhotoController {

    /** 올린 사진 파일은 저장한 뒤 바뀌지 않는다 */
    private static final Duration IMAGE_MAX_AGE = Duration.ofDays(365);

    private final SitePhotoService sitePhotoService;

    @GetMapping
    @Operation(summary = "Visible photo URLs per slot")
    public ResponseEntity<SitePhotoResponses.PublicView> list() {
        return ResponseEntity.ok().cacheControl(CacheControl.noCache()).body(sitePhotoService.publicView());
    }

    @GetMapping("/{id}/image")
    @Operation(summary = "Uploaded photo file")
    public ResponseEntity<byte[]> image(@PathVariable UUID id) {
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.maxAge(IMAGE_MAX_AGE).cachePublic().immutable())
                .body(sitePhotoService.loadImage(id));
    }
}
