package com.biddingagency.controller.admin;

import com.biddingagency.domain.rfp.dto.*;
import com.biddingagency.domain.rfp.service.RfpSampleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/** 관리자 성공 제안서 등록 Controller (CR-013 재설계) — 원본 통째 보관, 슬롯 분류 없음 */
@Slf4j
@RestController
@RequestMapping("/admin/rfp-samples")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - RFP Samples", description = "성공 제안서 등록 + 원본 파일 보관 (CR-013)")
public class RfpSampleAdminController {

    private final RfpSampleService rfpSampleService;

    @GetMapping
    @Operation(summary = "성공 제안서 목록")
    public ResponseEntity<Page<RfpSampleDto>> list(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(rfpSampleService.list(pageable));
    }

    @PostMapping
    @Operation(summary = "성공 제안서 메타 등록")
    public ResponseEntity<RfpSampleDto> create(@RequestBody RfpSampleCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rfpSampleService.create(req));
    }

    @GetMapping("/{id}")
    @Operation(summary = "성공 제안서 상세 (메타 + 원본 파일들)")
    public ResponseEntity<RfpSampleDetailDto> getDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(rfpSampleService.getDetail(id));
    }

    @PostMapping(value = "/{id}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "원본 파일 업로드 (로컬 저장)")
    public ResponseEntity<RfpSampleFileDto> uploadFile(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "isPws", defaultValue = "false") boolean isPws) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rfpSampleService.uploadFile(id, file, isPws));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "성공 제안서 삭제")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        rfpSampleService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/files/{fileId}")
    @Operation(summary = "파일 삭제")
    public ResponseEntity<Void> deleteFile(@PathVariable UUID id, @PathVariable UUID fileId) {
        rfpSampleService.deleteFile(id, fileId);
        return ResponseEntity.noContent().build();
    }
}
