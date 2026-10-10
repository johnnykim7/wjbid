package com.biddingagency.controller;

import com.biddingagency.domain.member.entity.DocumentCategory;
import com.biddingagency.domain.member.entity.MemberDocument;
import com.biddingagency.domain.member.service.MemberDocumentService;
import com.biddingagency.security.CustomUserDetails;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 회사 공통 서류함 (CR-125)
 *
 * member 종속 공통 서류 보관 + 재사용. 인증된 본인 member 기준.
 */
@Slf4j
@RestController
@RequestMapping("/member-documents")
@RequiredArgsConstructor
@Tag(name = "Member Documents", description = "회사 공통 서류함 (CR-125)")
public class MemberDocumentController {

    private final MemberDocumentService service;

    /** 서류 응답 DTO (LAZY 폭발 방지 — ClientDocumentDto 패턴) */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record MemberDocumentDto(
            String id,
            String category,
            String categoryLabel,
            boolean fixed,
            String fileName,
            Long fileSize,
            String contentType,
            String storageUrl,
            String displayName,
            String description,
            LocalDateTime createdAt
    ) {
        public static MemberDocumentDto from(MemberDocument d) {
            return new MemberDocumentDto(
                    d.getId() != null ? d.getId().toString() : null,
                    d.getCategory().name(),
                    d.getCategory().getDisplayName(),
                    d.getCategory().isFixed(),
                    d.getFileName(),
                    d.getFileSize(),
                    d.getContentType(),
                    d.getStorageUrl(),
                    d.getDisplayName(),
                    d.getDescription(),
                    d.getCreatedAt()
            );
        }
    }

    /** 고정 슬롯 메타 (서류 없어도 카드를 그릴 수 있도록 비어있는 슬롯도 내려줌) */
    public record FixedSlot(
            String category,
            String categoryLabel,
            MemberDocumentDto document // null = 미등록
    ) {}

    /** 서류함 전체 응답 — 고정 슬롯 6개(순서 고정) + 기타 목록 */
    public record VaultResponse(
            List<FixedSlot> fixedSlots,
            List<MemberDocumentDto> others
    ) {}

    @GetMapping
    @Operation(summary = "내 서류함 조회")
    public ResponseEntity<VaultResponse> vault(@AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID memberId = userDetails.getMember().getId();
        List<MemberDocument> docs = service.list(memberId);

        Map<DocumentCategory, MemberDocument> fixedByCategory = docs.stream()
                .filter(d -> d.getCategory().isFixed())
                .collect(Collectors.toMap(MemberDocument::getCategory, d -> d, (a, b) -> a));

        List<FixedSlot> fixedSlots = new ArrayList<>();
        for (DocumentCategory c : DocumentCategory.values()) {
            if (!c.isFixed()) continue;
            MemberDocument d = fixedByCategory.get(c);
            fixedSlots.add(new FixedSlot(c.name(), c.getDisplayName(),
                    d != null ? MemberDocumentDto.from(d) : null));
        }

        List<MemberDocumentDto> others = docs.stream()
                .filter(d -> !d.getCategory().isFixed())
                .map(MemberDocumentDto::from)
                .toList();

        return ResponseEntity.ok(new VaultResponse(fixedSlots, others));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "서류 업로드")
    public ResponseEntity<MemberDocumentDto> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("category") DocumentCategory category,
            @RequestParam(value = "displayName", required = false) String displayName,
            @RequestParam(value = "description", required = false) String description,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID memberId = userDetails.getMember().getId();
        MemberDocument saved = service.upload(memberId, category, displayName, description, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(MemberDocumentDto.from(saved));
    }

    @PutMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "서류 교체/수정")
    public ResponseEntity<MemberDocumentDto> replace(
            @PathVariable UUID id,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "displayName", required = false) String displayName,
            @RequestParam(value = "description", required = false) String description,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID memberId = userDetails.getMember().getId();
        MemberDocument updated = service.replace(memberId, id, displayName, description, file);
        return ResponseEntity.ok(MemberDocumentDto.from(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "서류 삭제")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID memberId = userDetails.getMember().getId();
        service.delete(memberId, id);
        return ResponseEntity.noContent().build();
    }
}
