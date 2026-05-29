package com.biddingagency.controller.admin;

import com.biddingagency.domain.notice.entity.Notice;
import com.biddingagency.domain.notice.service.NoticeService;
import com.biddingagency.domain.opportunity.dto.OpportunityAdminDto;
import com.biddingagency.domain.opportunity.dto.OpportunityAttachmentDto;
import com.biddingagency.domain.opportunity.entity.AttachmentDownloadStatus;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.entity.OpportunityAttachment;
import com.biddingagency.domain.opportunity.repository.OpportunityAttachmentRepository;
import com.biddingagency.domain.opportunity.service.OpportunityService;
import com.biddingagency.integration.storage.StorageService;
import com.biddingagency.security.CustomUserDetails;
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

    @GetMapping
    @Operation(summary = "원본 공고 목록 (관리자)", description = "SAM 수집 원본 + 첨부파일 수 + 공고문 생성 여부")
    public ResponseEntity<Page<OpportunityAdminDto>> listOpportunities(
            @PageableDefault(size = 20, sort = "lastModifiedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<OpportunityAdminDto> page = opportunityService.findAllActive(pageable)
                .map(opp -> {
                    long attachmentCount = attachmentRepository.countByOpportunityId(opp.getId());
                    long manualFetchCount = attachmentRepository.countByOpportunityIdAndDownloadStatus(
                            opp.getId(), AttachmentDownloadStatus.MANUAL_FETCH_REQUIRED);
                    int noticeCount = noticeService.findByOpportunityId(opp.getId()).size();
                    return OpportunityAdminDto.fromList(opp, attachmentCount, manualFetchCount, noticeCount);
                });
        return ResponseEntity.ok(page);
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
        Notice notice = noticeService.createNotice(id, userDetails.getMember().getId());
        log.info("공고문 생성(게이트①): opportunityId={}, noticeId={}", id, notice.getId());
        return ResponseEntity.accepted().body(Map.of(
                "status", "ACCEPTED",
                "message", "공고문 생성 및 한글화가 시작되었습니다.",
                "opportunityId", id.toString(),
                "noticeId", notice.getId().toString()
        ));
    }

    @PostMapping("/{id}/attachments")
    @Operation(summary = "첨부파일 수동 업로드 (CR-019)",
            description = "관리자가 외부서 가져온 첨부를 실제 저장. 동일 파일명의 '가져와야 함' 행이 있으면 갱신, 없으면 신규 SUCCESS 행.")
    public ResponseEntity<Map<String, String>> uploadAttachment(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) {
        Opportunity opp = opportunityService.findById(id);
        String fileName = file.getOriginalFilename();

        // CR-019: StorageService에 실제 저장
        String storageUrl = storageService.store("opportunity-attachments/" + id, file);

        // 동일 파일명의 "가져와야 함" 행이 있으면 그 행을 채워 SUCCESS 전이, 없으면 신규
        OpportunityAttachment attachment = attachmentRepository
                .findByOpportunityIdAndDownloadStatus(id, AttachmentDownloadStatus.MANUAL_FETCH_REQUIRED)
                .stream()
                .filter(a -> fileName != null && fileName.equals(a.getFileName()))
                .findFirst()
                .orElseGet(() -> OpportunityAttachment.builder()
                        .opportunity(opp)
                        .sourceUrl("admin-upload")
                        .build());

        attachment.applyUpload(fileName, file.getSize(), file.getContentType(), storageUrl);
        attachmentRepository.save(attachment);

        log.info("[CR-019] 관리자 첨부 업로드(SUCCESS): opportunityId={}, fileName={}, storageUrl={}",
                id, fileName, storageUrl);

        return ResponseEntity.ok(Map.of(
                "status", "UPLOADED",
                "fileName", fileName != null ? fileName : "",
                "opportunityId", id.toString()
        ));
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
