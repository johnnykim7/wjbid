package com.biddingagency.controller.admin;

import com.biddingagency.domain.rfp.dto.*;
import com.biddingagency.domain.rfp.service.RfpSampleService;
import com.biddingagency.domain.rfp.service.SlotAssignmentService;
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

import java.util.List;
import java.util.UUID;

/** 관리자 성공 제안서 등록/슬롯 배치 Controller (CR-013) */
@Slf4j
@RestController
@RequestMapping("/admin/rfp-samples")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - RFP Samples", description = "성공 제안서 등록 + 7슬롯 배치 (CR-013)")
public class RfpSampleAdminController {

    private final RfpSampleService rfpSampleService;
    private final SlotAssignmentService slotAssignmentService;

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
    @Operation(summary = "성공 제안서 상세 (파일 + 슬롯 배치 + 빈슬롯)")
    public ResponseEntity<RfpSampleDetailDto> getDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(rfpSampleService.getDetail(id));
    }

    @PostMapping(value = "/{id}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "원본 파일 업로드 (로컬 저장 + 슬롯 자동추정)")
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

    // ── 슬롯 배치 ──

    @GetMapping("/{id}/slots")
    @Operation(summary = "7슬롯 배치 현황 + 빈슬롯")
    public ResponseEntity<List<SlotStatusDto>> getSlots(@PathVariable UUID id) {
        return ResponseEntity.ok(rfpSampleService.buildSlotStatus(id));
    }

    @PutMapping("/{id}/slots/{slotCode}")
    @Operation(summary = "파일/섹션을 슬롯에 배치")
    public ResponseEntity<SlotAssignmentDto> assign(
            @PathVariable UUID id,
            @PathVariable String slotCode,
            @RequestBody SlotAssignRequest req) {
        return ResponseEntity.ok(slotAssignmentService.assign(id, slotCode, req));
    }

    @DeleteMapping("/{id}/slots/{assignmentId}")
    @Operation(summary = "슬롯 배치 해제")
    public ResponseEntity<Void> unassign(@PathVariable UUID id, @PathVariable UUID assignmentId) {
        slotAssignmentService.unassign(assignmentId);
        return ResponseEntity.noContent().build();
    }
}
