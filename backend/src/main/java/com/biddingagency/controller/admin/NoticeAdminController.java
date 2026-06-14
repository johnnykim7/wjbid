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

    @PostMapping("/{id}/cancel")
    @Operation(summary = "한글화/분석 강제 중단 (CR-039)",
            description = "ANALYZING으로 멈춘(stuck) 공고문을 강제 FAILED 전환. Aimbase 취소는 best-effort.")
    public ResponseEntity<Map<String, String>> cancel(@PathVariable UUID id) {
        boolean cancelled = noticeService.cancelAnalysis(id, "관리자 강제 중단");
        log.info("공고문 한글화 강제 중단: noticeId={}, cancelled={}", id, cancelled);
        return ResponseEntity.ok(Map.of(
                "status", cancelled ? "CANCELLED" : "SKIPPED",
                "generationStatus", cancelled ? "FAILED" : "UNCHANGED",
                "noticeId", id.toString()
        ));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "공고문 삭제 (CR-040)",
            description = "hard delete. VISIBLE(노출 중)·ANALYZING(분석 중)이면 409 거부. 원본 Opportunity는 보존.")
    public ResponseEntity<Map<String, String>> delete(@PathVariable UUID id) {
        try {
            noticeService.deleteNotice(id);
            log.info("공고문 삭제: noticeId={}", id);
            return ResponseEntity.ok(Map.of("status", "DELETED", "noticeId", id.toString()));
        } catch (IllegalStateException e) {
            // 가드 위반(노출 중/분석 중) → 409 + 사유
            log.warn("공고문 삭제 거부: noticeId={}, reason={}", id, e.getMessage());
            return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT)
                    .body(Map.of("status", "REJECTED", "reason", e.getMessage(), "noticeId", id.toString()));
        }
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
