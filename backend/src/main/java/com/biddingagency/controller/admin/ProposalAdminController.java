package com.biddingagency.controller.admin;

import com.biddingagency.domain.bid.service.AIWorkflowService;
import com.biddingagency.domain.proposal.dto.ProposalTreeDto;
import com.biddingagency.domain.proposal.entity.ProposalSection;
import com.biddingagency.domain.proposal.service.ProposalService;
import com.biddingagency.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * CR-030 — 제안서 Section 트리 관리 (관리자 콘솔).
 *
 * <p>좌측 chapter/section 트리 조회, section 본문 조회, 부분 재생성, LOCKED 잠금/해제.
 * 재생성은 CR-028 write-section 파이프라인을 section 단위로 비동기 호출한다.
 *
 * <p>검증 결과 패널(환각/분량)은 CR-031 verification_log 도입 후 별도 엔드포인트로 연결한다 —
 * 본 컨트롤러는 현재 placeholder 를 반환한다.
 */
@Slf4j
@RestController
@RequestMapping("/admin/bid-documents/{documentId}")
@RequiredArgsConstructor
@Tag(name = "Admin - Proposal Tree", description = "CR-030 제안서 section 트리 관리")
@PreAuthorize("hasRole('ADMIN')")
public class ProposalAdminController {

    private final ProposalService proposalService;
    private final AIWorkflowService aiWorkflowService;

    /** 좌측 트리 — chapter → section. */
    @GetMapping("/chapters")
    @Operation(summary = "Section 트리", description = "문서의 chapter/section 트리 조회")
    public ResponseEntity<ProposalTreeDto> getTree(@PathVariable UUID documentId) {
        List<ProposalTreeDto.ChapterNode> chapters = proposalService.getTree(documentId).stream()
                .map(cw -> ProposalTreeDto.ChapterNode.of(
                        cw.chapter(),
                        cw.sections().stream().map(ProposalTreeDto.SectionNode::of).toList()))
                .toList();
        return ResponseEntity.ok(new ProposalTreeDto(documentId, chapters));
    }

    /** 우측 패널 — 선택한 section 본문(block) + 메타. */
    @GetMapping("/sections/{sectionId}")
    @Operation(summary = "Section 상세", description = "선택한 section 의 본문 block 과 메타")
    public ResponseEntity<ProposalTreeDto.SectionDetail> getSection(
            @PathVariable UUID documentId,
            @PathVariable UUID sectionId) {
        ProposalSection section = proposalService.getSection(sectionId);
        return ResponseEntity.ok(ProposalTreeDto.SectionDetail.of(
                section, proposalService.getBlocks(sectionId)));
    }

    /**
     * 부분 재생성 — section 1개만 write-section WF 재호출 (비동기).
     * LOCKED section 은 거부 (사람이 다듬은 산출물 보호 — BIZ HUMAN_EDITED).
     */
    @PostMapping("/sections/{sectionId}/regenerate")
    @Operation(summary = "Section 재생성", description = "해당 section 본문만 write-section 파이프라인으로 재작성")
    public ResponseEntity<Map<String, Object>> regenerate(
            @PathVariable UUID documentId,
            @PathVariable UUID sectionId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ProposalSection section = proposalService.getSection(sectionId);
        if (section.isLocked()) {
            return ResponseEntity.status(409).body(Map.of(
                    "sectionId", sectionId.toString(),
                    "error", "LOCKED",
                    "message", "잠긴 section 은 재생성할 수 없습니다. 먼저 잠금을 해제하세요."
            ));
        }
        log.info("[CR-030] section 재생성 요청: sectionId={} by {}", sectionId, userDetails.getUsername());
        aiWorkflowService.writeSectionAsync(sectionId);
        return ResponseEntity.accepted().body(Map.of(
                "sectionId", sectionId.toString(),
                "message", "section 재작성을 시작했습니다. 완료 시 상태가 갱신됩니다."
        ));
    }

    /** LOCKED 잠금 — 이후 자동 트리거(검증실패/분량미달/Notice변경) 모두 차단. */
    @PostMapping("/sections/{sectionId}/lock")
    @Operation(summary = "Section 잠금", description = "사람이 다듬은 section 을 LOCKED 로 — 자동 재생성 차단")
    public ResponseEntity<ProposalTreeDto.SectionNode> lock(
            @PathVariable UUID documentId,
            @PathVariable UUID sectionId) {
        proposalService.lockSection(sectionId);
        return ResponseEntity.ok(ProposalTreeDto.SectionNode.of(proposalService.getSection(sectionId)));
    }

    /** 잠금 해제 — DRAFTED 로 복귀. */
    @PostMapping("/sections/{sectionId}/unlock")
    @Operation(summary = "Section 잠금 해제", description = "LOCKED → DRAFTED 복귀")
    public ResponseEntity<ProposalTreeDto.SectionNode> unlock(
            @PathVariable UUID documentId,
            @PathVariable UUID sectionId) {
        proposalService.unlockSection(sectionId);
        return ResponseEntity.ok(ProposalTreeDto.SectionNode.of(proposalService.getSection(sectionId)));
    }

    /**
     * 검증 결과 패널 — CR-031 verification_log 미도입. 현재 placeholder.
     * CR-031 구현 시 verification_log 조회로 교체한다.
     */
    @GetMapping("/sections/{sectionId}/verification")
    @Operation(summary = "검증 결과 (CR-031 예정)", description = "환각/분량 검증 결과 — CR-031 도입 후 연결")
    public ResponseEntity<Map<String, Object>> verification(
            @PathVariable UUID documentId,
            @PathVariable UUID sectionId) {
        return ResponseEntity.ok(Map.of(
                "sectionId", sectionId.toString(),
                "available", false,
                "message", "검증 파이프라인(CR-031) 도입 후 제공됩니다."
        ));
    }
}
