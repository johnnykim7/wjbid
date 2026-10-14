package com.biddingagency.controller.admin;

import com.biddingagency.domain.notice.service.NoticeService;
import com.biddingagency.domain.opportunity.dto.OpportunityAdminDto;
import com.biddingagency.domain.opportunity.dto.OpportunityAttachmentDto;
import com.biddingagency.domain.opportunity.entity.AttachmentDownloadStatus;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.entity.OpportunityAttachment;
import com.biddingagency.domain.opportunity.repository.OpportunityAttachmentRepository;
import com.biddingagency.domain.opportunity.service.AttachmentAutoDownloadService;
import com.biddingagency.domain.opportunity.service.AttachmentFiles;
import com.biddingagency.domain.opportunity.service.OpportunityAttachmentIngestService;
import com.biddingagency.domain.opportunity.service.OpportunityService;
import com.biddingagency.domain.opportunity.service.PieeSettings;
import com.biddingagency.integration.storage.StorageService;
import com.biddingagency.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 관리자 원본 공고(Opportunity) 관리 Controller — CR-016.
 *
 * 원본 = SAM 수집물(선별 풀). 여기서 관리자가 "공고문 만들기"(create-notice = 게이트①)로 선별.
 * 한글화/노출 관리는 NoticeAdminController(공고문 리스트) 소관.
 */
@Slf4j
@RestController
@RequestMapping("/admin/opportunities")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Opportunities", description = "원본 공고 선별 풀")
public class OpportunityAdminController {

    private final OpportunityService opportunityService;
    private final NoticeService noticeService;
    private final OpportunityAttachmentRepository attachmentRepository;
    private final StorageService storageService;
    private final AttachmentAutoDownloadService attachmentAutoDownloadService;
    private final com.biddingagency.domain.opportunity.service.OpportunityTranslationService translationService;
    private final OpportunityAttachmentIngestService attachmentIngestService;
    private final PieeSettings pieeSettings;

    @GetMapping
    @Operation(summary = "원본 공고 목록 (관리자)",
            description = "SAM 수집 원본 + 첨부파일 수 + 공고문 생성 여부. CR-118: keyword/type/hasAttachment 검색·필터.")
    public ResponseEntity<Page<OpportunityAdminDto>> listOpportunities(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean hasAttachment,
            @RequestParam(required = false) Boolean hasNotice,
            // 정렬은 쿼리에 고정(게시일 DESC, 2차 수집순 DESC) — Pageable sort 미지정
            @PageableDefault(size = 20) Pageable pageable) {
        Page<OpportunityAdminDto> page = opportunityService
                .searchAdminFiltered(keyword, type, hasAttachment, hasNotice, pageable)
                .map(opp -> {
                    long attachmentCount = attachmentRepository.countByOpportunityId(opp.getId());
                    long manualFetchCount = attachmentRepository.countByOpportunityIdAndDownloadStatus(
                            opp.getId(), AttachmentDownloadStatus.MANUAL_FETCH_REQUIRED);
                    int noticeCount = noticeService.findByOpportunityId(opp.getId()).size();
                    return OpportunityAdminDto.fromList(opp, attachmentCount, manualFetchCount, noticeCount,
                            pieeSettings.noticeUrl(opp.getSolicitationNumber()).orElse(null));
                });
        return ResponseEntity.ok(page);
    }

    @GetMapping("/types")
    @Operation(summary = "공고유형 목록 (CR-118)", description = "검색 셀렉트 옵션용 — 수집된 type/typeKo DISTINCT")
    public ResponseEntity<List<Map<String, String>>> listTypes() {
        return ResponseEntity.ok(opportunityService.findDistinctTypes());
    }

    @GetMapping("/{id}")
    @Operation(summary = "원본 공고 상세 (관리자)", description = "딸린 공고문 목록 포함")
    public ResponseEntity<OpportunityAdminDto> getOpportunity(@PathVariable UUID id) {
        Opportunity opp = opportunityService.findById(id);
        long attachmentCount = attachmentRepository.countByOpportunityId(id);
        long manualFetchCount = attachmentRepository.countByOpportunityIdAndDownloadStatus(
                id, AttachmentDownloadStatus.MANUAL_FETCH_REQUIRED);
        int noticeCount = noticeService.findByOpportunityId(id).size();
        return ResponseEntity.ok(OpportunityAdminDto.from(opp, attachmentCount, manualFetchCount, noticeCount,
                pieeSettings.noticeUrl(opp.getSolicitationNumber()).orElse(null)));
    }

    @PostMapping("/manual")
    @Operation(summary = "테스트용 수동 공고 등록 (CR-120)",
            description = "SAM 수집 없이 빈 원본 공고 1건을 생성한다. 이후 첨부(PWS) 업로드 → 공고문 분석 → 제안서 생성 " +
                    "파이프라인을 시뮬레이션하기 위한 테스트 전용. noticeId 미입력 시 TEST-{timestamp} 자동 생성, 중복이면 409.")
    public ResponseEntity<Map<String, String>> createManual(@RequestBody Map<String, String> body) {
        try {
            Opportunity opp = opportunityService.createManual(
                    body.get("noticeId"), body.get("title"));
            log.info("[CR-120] 테스트용 수동 공고 등록: opportunityId={}, noticeId={}",
                    opp.getId(), opp.getNoticeId());
            return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(Map.of(
                    "status", "CREATED",
                    "opportunityId", opp.getId().toString(),
                    "noticeId", opp.getNoticeId()
            ));
        } catch (IllegalArgumentException e) {
            log.warn("[CR-120] 수동 공고 등록 거부: reason={}", e.getMessage());
            return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT)
                    .body(Map.of("status", "REJECTED", "reason", e.getMessage()));
        }
    }

    @PostMapping("/{id}/create-notice")
    @Operation(summary = "공고문 만들기 (게이트①)", description = "원본을 선별해 공고문 생성 + 한글화/요약 트리거")
    public ResponseEntity<Map<String, String>> createNotice(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            UUID noticeId = noticeService.createNotice(id, userDetails.getMember().getId());
            log.info("공고문 생성(게이트①): opportunityId={}, noticeId={}", id, noticeId);
            return ResponseEntity.accepted().body(Map.of(
                    "status", "ACCEPTED",
                    "message", "공고문 생성 및 한글화가 시작되었습니다.",
                    "opportunityId", id.toString(),
                    "noticeId", noticeId.toString()
            ));
        } catch (IllegalStateException e) {
            log.warn("공고문 생성 거부: opportunityId={}, reason={}", id, e.getMessage());
            return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT)
                    .body(Map.of("status", "REJECTED", "reason", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "원본 공고 소프트 삭제 (CR-042)",
            description = "목록·검색·고객 노출에서 제외(soft delete, 원본 row 보존). " +
                    "연결된 공고문이 노출 중(VISIBLE)·분석 중(ANALYZING)이면 409 거부. " +
                    "재수집 시 동일 noticeId는 다시 들어오지 않음(삭제 상태 유지).")
    public ResponseEntity<Map<String, String>> deleteOpportunity(@PathVariable UUID id) {
        try {
            opportunityService.softDelete(id);
            log.info("[CR-042] 원본 공고 소프트 삭제: opportunityId={}", id);
            return ResponseEntity.ok(Map.of("status", "DELETED", "opportunityId", id.toString()));
        } catch (IllegalStateException e) {
            log.warn("[CR-042] 원본 공고 삭제 거부: opportunityId={}, reason={}", id, e.getMessage());
            return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT)
                    .body(Map.of("status", "REJECTED", "reason", e.getMessage(), "opportunityId", id.toString()));
        }
    }

    @PostMapping("/{id}/attachments")
    @Operation(summary = "첨부파일 수동 업로드 (CR-019, CR-037)",
            description = "관리자가 외부서 가져온 첨부를 실제 저장. 다중 파일 선택 지원. " +
                    "ZIP 파일은 BE가 자동 해제하여 각 엔트리를 개별 첨부로 등록(폴더 구조 평탄화 — 파일명만 사용). " +
                    "동일 파일명의 '가져와야 함' 행이 있으면 갱신, 없으면 신규 SUCCESS 행.")
    public ResponseEntity<Map<String, Object>> uploadAttachment(
            @PathVariable UUID id,
            @RequestParam("files") List<MultipartFile> files) {
        List<String> uploaded = attachmentIngestService.ingestUploads(id, files);
        return ResponseEntity.ok(Map.of(
                "status", "UPLOADED",
                "count", uploaded.size(),
                "fileNames", uploaded,
                "opportunityId", id.toString()
        ));
    }

    @PostMapping("/{id}/attachments/{attachmentId}/auto-fetch")
    @Operation(summary = "첨부 자동 다운로드 시도 (CR-025)",
            description = "SAM 자체호스팅 첨부를 SAM API 키로 직접 다운로드해 적재. 동기 실행, 성공/실패 즉시 응답. 화이트리스트 비매칭이면 SKIPPED.")
    public ResponseEntity<Map<String, Object>> autoFetchAttachment(
            @PathVariable UUID id,
            @PathVariable UUID attachmentId) {
        OpportunityAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("첨부 없음: " + attachmentId));
        if (!attachment.getOpportunity().getId().equals(id)) {
            throw new IllegalArgumentException("첨부가 해당 공고 소속이 아님: " + attachmentId);
        }
        if (!attachmentAutoDownloadService.isAutoDownloadable(attachment.getSourceUrl())) {
            return ResponseEntity.ok(Map.of(
                    "status", "SKIPPED",
                    "reason", "화이트리스트 비매칭 — 자동 다운로드 불가 (외부 도메인). 수동 업로드 필요.",
                    "attachmentId", attachmentId.toString()
            ));
        }
        boolean ok = attachmentAutoDownloadService.tryAutoFetch(attachmentId);
        OpportunityAttachment after = attachmentRepository.findById(attachmentId).orElse(attachment);
        return ResponseEntity.ok(Map.of(
                "status", ok ? "SUCCESS" : "FAILED",
                "downloadStatus", after.getDownloadStatus().name(),
                "failureReason", after.getFailureReason() != null ? after.getFailureReason() : "",
                "attachmentId", attachmentId.toString()
        ));
    }

    @GetMapping("/{id}/attachments/{attachmentId}/download")
    @Operation(summary = "첨부파일 다운로드 (CR-034)",
            description = "저장된 첨부(storageUrl 보유 — 수동 업로드 + SAM 자동 다운로드 성공분)를 바이트로 내려준다. 미저장 첨부는 404.")
    public ResponseEntity<byte[]> downloadAttachment(
            @PathVariable UUID id,
            @PathVariable UUID attachmentId) {
        OpportunityAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("첨부 없음: " + attachmentId));
        if (!attachment.getOpportunity().getId().equals(id)) {
            throw new IllegalArgumentException("첨부가 해당 공고 소속이 아님: " + attachmentId);
        }
        String storageUrl = attachment.getStorageUrl();
        if (storageUrl == null || storageUrl.isBlank()) {
            return ResponseEntity.notFound().build();
        }
        byte[] content = storageService.load(storageUrl);

        String fileName = attachment.getFileName() != null ? attachment.getFileName() : "attachment";
        org.springframework.http.ContentDisposition cd = org.springframework.http.ContentDisposition
                .attachment().filename(fileName, java.nio.charset.StandardCharsets.UTF_8).build();
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentDisposition(cd);
        String ct = attachment.getContentType();
        headers.setContentType(ct != null && !ct.isBlank()
                ? org.springframework.http.MediaType.parseMediaType(ct)
                : org.springframework.http.MediaType.APPLICATION_OCTET_STREAM);
        return new ResponseEntity<>(content, headers, org.springframework.http.HttpStatus.OK);
    }

    @DeleteMapping("/{id}/attachments/{attachmentId}")
    @Operation(summary = "수동 업로드 첨부 삭제 (CR-034)",
            description = "관리자가 수동 업로드한 첨부(sourceUrl='admin-upload')만 삭제. SAM 수집 첨부는 403으로 거부(원본 보존).")
    public ResponseEntity<Map<String, String>> deleteAttachment(
            @PathVariable UUID id,
            @PathVariable UUID attachmentId) {
        OpportunityAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("첨부 없음: " + attachmentId));
        if (!attachment.getOpportunity().getId().equals(id)) {
            throw new IllegalArgumentException("첨부가 해당 공고 소속이 아님: " + attachmentId);
        }
        // CR-034: SAM 수집 첨부(원본)는 삭제 불가 — 관리자 수동 업로드분만 삭제 허용
        if (!AttachmentFiles.ADMIN_UPLOAD_SOURCE.equals(attachment.getSourceUrl())) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).body(Map.of(
                    "status", "FORBIDDEN",
                    "reason", "SAM 수집 첨부는 삭제할 수 없습니다. 관리자가 직접 업로드한 첨부만 삭제 가능합니다.",
                    "attachmentId", attachmentId.toString()
            ));
        }
        attachmentRepository.delete(attachment);
        log.info("[CR-034] 수동 업로드 첨부 삭제: opportunityId={}, attachmentId={}, fileName={}",
                id, attachmentId, attachment.getFileName());
        return ResponseEntity.ok(Map.of(
                "status", "DELETED",
                "attachmentId", attachmentId.toString()
        ));
    }

    @PostMapping("/{id}/retranslate-description")
    @Operation(summary = "본문 한글 번역 (CR-022 재구현)",
            description = "원본 공고 본문(description)을 LLM으로 한글 번역. 자동 번역이 실패했거나 누락된 경우 관리자가 수동으로 호출. 동기 실행.")
    public ResponseEntity<Map<String, Object>> retranslateDescription(@PathVariable UUID id) {
        boolean ok = translationService.translate(id);
        Opportunity opp = opportunityService.findById(id);
        log.info("[CR-022] 본문 번역 요청: opportunityId={}, success={}", id, ok);
        return ResponseEntity.ok(Map.of(
                "status", ok ? "TRANSLATED" : "FAILED_OR_EMPTY",
                "descriptionKo", opp.getDescriptionSummaryKo() != null ? opp.getDescriptionSummaryKo() : "",
                "translatedAt", opp.getTranslatedAt() != null ? opp.getTranslatedAt().toString() : ""
        ));
    }

    @PostMapping("/{id}/translate-title")
    @Operation(summary = "제목만 한글 번역 (리스트 일괄 번역용)",
            description = "원본 공고 제목만 LLM으로 번역. 본문(noticedesc) fetch를 하지 않아 SAM 쿼터를 소진하지 않는다. " +
                    "리스트 화면의 '현재 페이지 미번역분 일괄 번역' 버튼이 호출. 동기 실행. 이미 번역된 제목은 skip.")
    public ResponseEntity<Map<String, Object>> translateTitle(@PathVariable UUID id) {
        translationService.translateTitle(id);
        Opportunity opp = opportunityService.findById(id);
        boolean translated = opp.getTranslatedAt() != null;
        log.info("[CR-022] 제목 번역 요청: opportunityId={}, translated={}", id, translated);
        return ResponseEntity.ok(Map.of(
                "status", translated ? "TRANSLATED" : "FAILED_OR_EMPTY",
                "titleKo", opp.getTitleKo() != null ? opp.getTitleKo() : "",
                "translatedAt", opp.getTranslatedAt() != null ? opp.getTranslatedAt().toString() : ""
        ));
    }

    @PatchMapping("/{id}/piee-link-broken")
    @Operation(summary = "PIEE 링크 오류 표식 토글 (CR-043)",
            description = "이 공고의 PIEE solNo 직링크가 오류(메인으로 리다이렉트 등)일 때 관리자가 수동으로 표시. " +
                    "목록·상세에 경고를 노출해 관리자 헛클릭을 막는다. 우리 코드/사용자 PC 문제가 아니라 해당 공고의 PIEE 게시 상태 문제.")
    public ResponseEntity<Map<String, Object>> setPieeLinkBroken(
            @PathVariable UUID id,
            @RequestBody Map<String, Boolean> body) {
        boolean broken = Boolean.TRUE.equals(body.get("broken"));
        boolean result = opportunityService.setPieeLinkBroken(id, broken);
        return ResponseEntity.ok(Map.of("pieeLinkBroken", result));
    }

    @GetMapping("/{id}/attachments")
    @Operation(summary = "원본 공고 첨부 목록 (관리자, CR-019)",
            description = "각 첨부의 다운로드 상태(SUCCESS/MANUAL_FETCH_REQUIRED 등)와 외부 원본 링크 포함")
    public ResponseEntity<List<OpportunityAttachmentDto>> listAttachments(@PathVariable UUID id) {
        List<OpportunityAttachmentDto> attachments = attachmentRepository.findByOpportunityId(id)
                .stream()
                .map(OpportunityAttachmentDto::from)
                .toList();
        return ResponseEntity.ok(attachments);
    }
}
