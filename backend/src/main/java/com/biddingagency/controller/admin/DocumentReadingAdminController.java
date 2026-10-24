package com.biddingagency.controller.admin;

import com.biddingagency.domain.reading.dto.DocumentReadingDto.Detail;
import com.biddingagency.domain.reading.dto.DocumentReadingDto.Overview;
import com.biddingagency.domain.reading.service.DocumentReadingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Admin - Document Reading", description = "CR-169 고객 서류 읽기")
@PreAuthorize("hasRole('ADMIN')")
public class DocumentReadingAdminController {

    private final DocumentReadingService readingService;

    @GetMapping("/bid-requests/{id}/document-readings")
    @Operation(summary = "고객 서류 읽기 후보 목록")
    public ResponseEntity<Overview> list(@PathVariable UUID id) {
        return ResponseEntity.ok(readingService.overview(id));
    }

    @GetMapping("/document-readings/{readingId}")
    @Operation(summary = "고객 서류 사실 목록")
    public ResponseEntity<Detail> detail(@PathVariable UUID readingId) {
        return ResponseEntity.ok(readingService.detail(readingId));
    }

    @PostMapping("/bid-requests/{id}/document-readings/run")
    @Operation(summary = "고객 서류 다시 읽기", description = "readingIds 가 비면 읽음이 아닌 후보 전부, force 면 읽음도 다시 읽는다")
    public ResponseEntity<Overview> run(@PathVariable UUID id, @RequestBody(required = false) RunRequest request) {
        List<UUID> readingIds = request != null ? request.readingIds() : null;
        boolean force = request != null && Boolean.TRUE.equals(request.force());
        return ResponseEntity.ok(readingService.rerun(id, readingIds, force));
    }

    public record RunRequest(List<UUID> readingIds, Boolean force) {
    }
}
