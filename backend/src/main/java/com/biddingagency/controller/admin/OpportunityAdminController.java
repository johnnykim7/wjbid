package com.biddingagency.controller.admin;

import com.biddingagency.domain.notice.service.NoticeService;
import com.biddingagency.domain.opportunity.dto.OpportunityAdminDto;
import com.biddingagency.domain.opportunity.dto.OpportunityAttachmentDto;
import com.biddingagency.domain.opportunity.entity.AttachmentDownloadStatus;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.entity.OpportunityAttachment;
import com.biddingagency.domain.opportunity.repository.OpportunityAttachmentRepository;
import com.biddingagency.domain.opportunity.service.AttachmentAutoDownloadService;
import com.biddingagency.domain.opportunity.service.OpportunityService;
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

    @GetMapping
    @Operation(summary = "원본 공고 목록 (관리자)",
            description = "SAM 수집 원본 + 첨부파일 수 + 공고문 생성 여부. CR-118: keyword/type/hasAttachment 검색·필터.")
    public ResponseEntity<Page<OpportunityAdminDto>> listOpportunities(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean hasAttachment,
            // 정렬은 쿼리에 고정(게시일 DESC, 2차 수집순 DESC) — Pageable sort 미지정
            @PageableDefault(size = 20) Pageable pageable) {
        Page<OpportunityAdminDto> page = opportunityService
                .searchAdminFiltered(keyword, type, hasAttachment, pageable)
                .map(opp -> {
                    long attachmentCount = attachmentRepository.countByOpportunityId(opp.getId());
                    long manualFetchCount = attachmentRepository.countByOpportunityIdAndDownloadStatus(
                            opp.getId(), AttachmentDownloadStatus.MANUAL_FETCH_REQUIRED);
                    int noticeCount = noticeService.findByOpportunityId(opp.getId()).size();
                    return OpportunityAdminDto.fromList(opp, attachmentCount, manualFetchCount, noticeCount);
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
        return ResponseEntity.ok(OpportunityAdminDto.from(opp, attachmentCount, manualFetchCount, noticeCount));
    }

    @PostMapping("/{id}/create-notice")
    @Operation(summary = "공고문 만들기 (게이트①)", description = "원본을 선별해 공고문 생성 + 한글화/요약 트리거")
    public ResponseEntity<Map<String, String>> createNotice(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID noticeId = noticeService.createNotice(id, userDetails.getMember().getId());
        log.info("공고문 생성(게이트①): opportunityId={}, noticeId={}", id, noticeId);
        return ResponseEntity.accepted().body(Map.of(
                "status", "ACCEPTED",
                "message", "공고문 생성 및 한글화가 시작되었습니다.",
                "opportunityId", id.toString(),
                "noticeId", noticeId.toString()
        ));
    }

    @PostMapping("/{id}/attachments")
    @Operation(summary = "첨부파일 수동 업로드 (CR-019, CR-037)",
            description = "관리자가 외부서 가져온 첨부를 실제 저장. 다중 파일 선택 지원. " +
                    "ZIP 파일은 BE가 자동 해제하여 각 엔트리를 개별 첨부로 등록(폴더 구조 평탄화 — 파일명만 사용). " +
                    "동일 파일명의 '가져와야 함' 행이 있으면 갱신, 없으면 신규 SUCCESS 행.")
    public ResponseEntity<Map<String, Object>> uploadAttachment(
            @PathVariable UUID id,
            @RequestParam("files") List<MultipartFile> files) {
        Opportunity opp = opportunityService.findById(id);
        java.util.List<String> uploaded = new java.util.ArrayList<>();

        for (MultipartFile file : files) {
            String originalName = file.getOriginalFilename();
            if (isZip(originalName, file.getContentType())) {
                // CR-037: ZIP 자동 해제 — 각 엔트리를 개별 첨부로 등록 (평탄화)
                try (java.util.zip.ZipInputStream zis =
                             new java.util.zip.ZipInputStream(file.getInputStream())) {
                    java.util.zip.ZipEntry entry;
                    while ((entry = zis.getNextEntry()) != null) {
                        if (entry.isDirectory()) {
                            zis.closeEntry();
                            continue;
                        }
                        String entryName = flatten(entry.getName());
                        if (entryName == null || entryName.isBlank()) {
                            zis.closeEntry();
                            continue;
                        }
                        byte[] content = zis.readAllBytes();
                        zis.closeEntry();
                        if (content.length == 0) continue;
                        String storageUrl = storageService.store(
                                "opportunity-attachments/" + id, entryName, content);
                        saveUploadedAttachment(id, opp, entryName, (long) content.length,
                                guessContentType(entryName), storageUrl);
                        uploaded.add(entryName);
                        log.info("[CR-037] ZIP 엔트리 업로드: opportunityId={}, zip={}, entry={}, size={}",
                                id, originalName, entryName, content.length);
                    }
                } catch (java.io.IOException e) {
                    log.error("[CR-037] ZIP 해제 실패: opportunityId={}, zip={}", id, originalName, e);
                    throw new IllegalArgumentException("ZIP 파일 해제 실패: " + originalName);
                }
            } else {
                // 일반 단일 파일 (기존 CR-019 경로)
                String fileName = flatten(originalName);
                String storageUrl = storageService.store("opportunity-attachments/" + id, file);
                saveUploadedAttachment(id, opp, fileName, file.getSize(),
                        file.getContentType(), storageUrl);
                uploaded.add(fileName != null ? fileName : "");
                log.info("[CR-019] 관리자 첨부 업로드(SUCCESS): opportunityId={}, fileName={}, storageUrl={}",
                        id, fileName, storageUrl);
            }
        }

        return ResponseEntity.ok(Map.of(
                "status", "UPLOADED",
                "count", uploaded.size(),
                "fileNames", uploaded,
                "opportunityId", id.toString()
        ));
    }

    /** 업로드된 첨부 1건을 저장 — 동일 파일명의 '가져와야 함' 행이 있으면 채워 SUCCESS 전이, 없으면 신규. */
    private void saveUploadedAttachment(UUID id, Opportunity opp, String fileName,
                                        Long fileSize, String contentType, String storageUrl) {
        OpportunityAttachment attachment = attachmentRepository
                .findByOpportunityIdAndDownloadStatus(id, AttachmentDownloadStatus.MANUAL_FETCH_REQUIRED)
                .stream()
                .filter(a -> fileName != null && fileName.equals(a.getFileName()))
                .findFirst()
                .orElseGet(() -> OpportunityAttachment.builder()
                        .opportunity(opp)
                        .sourceUrl("admin-upload")
                        .build());
        attachment.applyUpload(fileName, fileSize, contentType, storageUrl);
        attachmentRepository.save(attachment);
    }

    /** CR-037: ZIP 여부 판별 — 확장자 또는 content-type. */
    private boolean isZip(String fileName, String contentType) {
        if (fileName != null && fileName.toLowerCase().endsWith(".zip")) return true;
        return contentType != null
                && (contentType.equals("application/zip")
                || contentType.equals("application/x-zip-compressed"));
    }

    /** CR-037: 경로 평탄화 — 마지막 세그먼트(파일명)만 사용. zip-slip 자동 회피. */
    private String flatten(String name) {
        if (name == null) return null;
        String normalized = name.replace('\\', '/');
        int idx = normalized.lastIndexOf('/');
        return idx >= 0 ? normalized.substring(idx + 1) : normalized;
    }

    /** 파일명 확장자로 content-type 추정 (ZIP 엔트리는 MultipartFile content-type이 없으므로). */
    private String guessContentType(String fileName) {
        if (fileName == null) return "application/octet-stream";
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".doc")) return "application/msword";
        if (lower.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (lower.endsWith(".xls")) return "application/vnd.ms-excel";
        if (lower.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        if (lower.endsWith(".txt")) return "text/plain";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        return "application/octet-stream";
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
        if (!"admin-upload".equals(attachment.getSourceUrl())) {
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
