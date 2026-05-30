package com.biddingagency.controller.admin;

import com.biddingagency.domain.notice.dto.NoticeAdminDto;
import com.biddingagency.domain.notice.entity.Notice;
import com.biddingagency.domain.notice.service.NoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * 관리자 공고문(Notice) 관리 Controller — CR-016.
 *
 * 공고문 = 원본에서 선별·한글화된 산출물. 검수 + 노출 토글(게이트②) + 재생성/보정.
 */
@Slf4j
@RestController
@RequestMapping("/admin/notices")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Notices", description = "공고문 관리 (한글화 검수, 노출)")
public class NoticeAdminController {

    private final NoticeService noticeService;

    @GetMapping
    @Operation(summary = "공고문 목록 (관리자)", description = "한글화 상태, 노출 상태 포함")
    public ResponseEntity<Page<NoticeAdminDto>> listNotices(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<NoticeAdminDto> page = noticeService.findAllAsDto(pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "공고문 상세 (관리자)", description = "한글화 결과 포함")
    public ResponseEntity<NoticeAdminDto> getNotice(@PathVariable UUID id) {
        return ResponseEntity.ok(noticeService.findByIdAsDto(id));
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "공고문 노출 (게이트②, 검수 완료 → 고객 노출)")
    public ResponseEntity<Map<String, String>> publish(@PathVariable UUID id) {
        Notice notice = noticeService.publish(id);
        log.info("공고문 노출: noticeId={}", id);
        return ResponseEntity.ok(Map.of(
                "status", "PUBLISHED",
                "visibility", notice.getVisibility().name(),
                "noticeId", id.toString()
        ));
    }

    @PostMapping("/{id}/hide")
    @Operation(summary = "공고문 비노출 (VISIBLE → HIDDEN)")
    public ResponseEntity<Map<String, String>> hide(@PathVariable UUID id) {
        Notice notice = noticeService.hide(id);
        log.info("공고문 비노출: noticeId={}", id);
        return ResponseEntity.ok(Map.of(
                "status", "HIDDEN",
                "visibility", notice.getVisibility().name(),
                "noticeId", id.toString()
        ));
    }

    @PostMapping("/{id}/regenerate")
    @Operation(summary = "한글화 재생성")
    public ResponseEntity<Map<String, String>> regenerate(@PathVariable UUID id) {
        noticeService.regenerate(id);
        log.info("공고문 한글화 재생성: noticeId={}", id);
        return ResponseEntity.accepted().body(Map.of(
                "status", "ACCEPTED",
                "message", "한글화 재생성이 시작되었습니다.",
                "noticeId", id.toString()
        ));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "한글화 결과 보정 (관리자 수동)")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, String>> updateResult(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body) {
        String koreanTitle = (String) body.get("koreanTitle");
        Map<String, Object> summaryJson = (Map<String, Object>) body.get("summaryJson");
        Map<String, Object> documentFormatsJson = (Map<String, Object>) body.get("documentFormatsJson");
        Map<String, Object> requiredDocumentsJson = (Map<String, Object>) body.get("requiredDocumentsJson");
        Map<String, Object> llmPromptPresetJson = (Map<String, Object>) body.get("llmPromptPresetJson");
        Map<String, Object> contentJson = (Map<String, Object>) body.get("contentJson");

        noticeService.updateResult(id, koreanTitle, summaryJson, documentFormatsJson, requiredDocumentsJson, llmPromptPresetJson, contentJson);
        log.info("공고문 한글화 결과 보정: noticeId={}", id);
        return ResponseEntity.ok(Map.of("status", "UPDATED", "noticeId", id.toString()));
    }
}
