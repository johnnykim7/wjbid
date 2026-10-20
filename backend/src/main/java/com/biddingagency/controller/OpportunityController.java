package com.biddingagency.controller;

import com.biddingagency.domain.notice.service.NoticeService;
import com.biddingagency.domain.opportunity.dto.OpportunityDto;
import com.biddingagency.domain.opportunity.dto.CustomerAttachmentDto;
import com.biddingagency.domain.opportunity.entity.OpportunityRequirementItem;
import com.biddingagency.domain.opportunity.repository.OpportunityRequirementItemRepository;
import com.biddingagency.domain.opportunity.service.CustomerAttachmentService;
import com.biddingagency.domain.opportunity.service.OpportunityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.biddingagency.domain.member.service.MembershipService;
import com.biddingagency.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Opportunity Controller (고객) — CR-016.
 *
 * 고객 노출 단위 = 공고문(Notice, 노출된 것만). id = noticeId.
 * 원본(Opportunity)은 관리자 선별 풀이라 고객에 직접 노출하지 않음.
 * CR-157: 공고문 내용(목록, 상세, 최근, 요구 서류)은 회원만(BIZ-071). 원본 공고 상세와 첨부는 모두에게 연다.
 */
@Slf4j
@RestController
@RequestMapping("/opportunities")
@RequiredArgsConstructor
@Tag(name = "Opportunities", description = "고객 공고문 조회")
public class OpportunityController {

    private final OpportunityService opportunityService;
    private final NoticeService noticeService;
    private final OpportunityRequirementItemRepository requirementItemRepository;
    private final CustomerAttachmentService customerAttachmentService;
    private final MembershipService membershipService;

    /**
     * 노출 공고문 목록 (CR-016)
     */
    @GetMapping
    @Operation(summary = "공고문 목록", description = "노출(VISIBLE) 공고문, 한글화 제목 포함. CR-009: 최신순 + 마감 공고 기본 제외")
    public ResponseEntity<Page<OpportunityDto>> listOpportunities(
            @RequestParam(defaultValue = "false") boolean includeExpired,
            // 정렬은 쿼리에 고정(게시일 DESC, 2차 수집순 DESC) — Pageable sort 미지정
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        membershipService.requireMember(memberIdOf(userDetails));
        Page<OpportunityDto> notices = noticeService.findVisible(includeExpired, pageable)
                .map(OpportunityDto::fromNotice);
        return ResponseEntity.ok(notices);
    }

    /**
     * 공고문 상세 (id = noticeId)
     */
    @GetMapping("/{id}")
    @Operation(summary = "공고문 상세", description = "노출 공고문 상세 + 한글화 결과")
    public ResponseEntity<OpportunityDto> getOpportunity(@PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        membershipService.requireMember(memberIdOf(userDetails));
        return noticeService.findVisibleDetail(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 원본 공고 상세 (CR-152, id = opportunityId). 공개 공고문이 없는 모은 공고도 우리 상세로 연다
     */
    @GetMapping("/origin/{opportunityId}")
    @Operation(summary = "원본 공고 상세", description = "모은 원본 공고 하나. 같은 공고의 공개 공고문이 있으면 publicNoticeId 를 싣는다")
    public ResponseEntity<OpportunityDto> getOrigin(@PathVariable UUID opportunityId) {
        return noticeService.findOriginDetail(opportunityId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get opportunity requirements
     */
    @GetMapping("/{id}/requirements")
    @Operation(summary = "공고 요구사항 목록")
    public ResponseEntity<List<OpportunityRequirementItem>> getRequirements(@PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        membershipService.requireMember(memberIdOf(userDetails));
        opportunityService.findById(id); // existence check
        return ResponseEntity.ok(requirementItemRepository.findByOpportunityIdAndReqIdIsNullOrderBySortOrderAsc(id)); // CR-159 BIZ-075 제출 서류 줄만
    }

    @GetMapping("/{id}/attachments")
    @Operation(summary = "공고 첨부파일 목록 (CR-168)", description = "받아 둔 첨부만 고객에게 줄 값으로 준다. id 는 모은 공고 id")
    public ResponseEntity<List<CustomerAttachmentDto>> getAttachments(@PathVariable UUID id) {
        return ResponseEntity.ok(customerAttachmentService.list(id));
    }

    @GetMapping("/{id}/attachments/{attachmentId}/download")
    @Operation(summary = "받아 둔 첨부 내려받기 (CR-168)", description = "로그인한 고객이면 회원 구분 없음(BIZ-084)")
    public ResponseEntity<byte[]> downloadAttachment(
            @PathVariable UUID id,
            @PathVariable UUID attachmentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return customerAttachmentService.load(memberIdOf(userDetails), id, attachmentId)
                .map(OpportunityController::fileResponse)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Get recently posted opportunities
     */
    @GetMapping("/recent")
    @Operation(summary = "Recently posted", description = "Get recently posted opportunities")
    public ResponseEntity<List<OpportunityDto>> recentlyPosted(
            @RequestParam(defaultValue = "30") int days,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        membershipService.requireMember(memberIdOf(userDetails));
        log.debug("Fetching opportunities posted in last {} days", days);
        List<OpportunityDto> opportunities = noticeService.findVisibleRecentlyPosted(days)
                .stream().map(OpportunityDto::fromNotice).toList();
        return ResponseEntity.ok(opportunities);
    }

    private static ResponseEntity<byte[]> fileResponse(CustomerAttachmentService.Download file) {
        HttpHeaders headers = new HttpHeaders();
        String fileName = file.fileName() != null ? file.fileName() : "attachment";
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8).build());
        String ct = file.contentType();
        headers.setContentType(ct != null && !ct.isBlank()
                ? MediaType.parseMediaType(ct) : MediaType.APPLICATION_OCTET_STREAM);
        return new ResponseEntity<>(file.content(), headers, HttpStatus.OK);
    }

    // 공개 주소라 토큰이 없거나 지났으면 로그인 정보가 비어 들어온다
    private static UUID memberIdOf(CustomUserDetails userDetails) {
        return userDetails != null ? userDetails.getMember().getId() : null;
    }
}
