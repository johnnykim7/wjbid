package com.biddingagency.controller.admin;

import com.biddingagency.domain.sitephoto.service.SitePhotoService;
import com.biddingagency.dto.SitePhotoResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * 고객 화면 사진 관리 (CR-153, BID-SITE-001)
 */
@RestController
@RequestMapping("/admin/site-photos")
@RequiredArgsConstructor
@Tag(name = "Admin - Site photos", description = "Photo bundles for customer screen slots")
@PreAuthorize("hasRole('ADMIN')")
public class SitePhotoAdminController {

    private final SitePhotoService sitePhotoService;

    @GetMapping
    @Operation(summary = "Slots with all photos and upload rules")
    public ResponseEntity<SitePhotoResponses.AdminView> list() {
        return ResponseEntity.ok(sitePhotoService.adminView());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a photo to a slot")
    public ResponseEntity<SitePhotoResponses.AdminPhoto> upload(@RequestParam("slotKey") String slotKey,
                                                                @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sitePhotoService.upload(slotKey, file));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Show or hide a photo")
    public ResponseEntity<SitePhotoResponses.AdminPhoto> changeVisible(@PathVariable UUID id,
                                                                       @RequestBody SitePhotoResponses.VisibleRequest request) {
        return ResponseEntity.ok(sitePhotoService.changeVisible(id, request.visible()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a photo")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        sitePhotoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
