package com.biddingagency.controller;

import com.biddingagency.domain.bookmark.service.BookmarkService;
import com.biddingagency.domain.opportunity.dto.OpportunityDto;
import com.biddingagency.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/bookmarks")
@RequiredArgsConstructor
@Tag(name = "Bookmarks", description = "관심 공고 관리")
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @GetMapping
    @Operation(summary = "내 북마크 목록")
    public ResponseEntity<Page<OpportunityDto>> getMyBookmarks(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable) {
        UUID memberId = userDetails.getMember().getId();
        Page<OpportunityDto> page = bookmarkService.findByMember(memberId, pageable)
                .map(b -> OpportunityDto.from(b.getOpportunity()));
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{opportunityId}/status")
    @Operation(summary = "북마크 여부 확인")
    public ResponseEntity<Map<String, Boolean>> checkBookmark(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID opportunityId) {
        boolean bookmarked = bookmarkService.isBookmarked(
                userDetails.getMember().getId(), opportunityId);
        return ResponseEntity.ok(Map.of("bookmarked", bookmarked));
    }

    @PostMapping("/{opportunityId}")
    @Operation(summary = "북마크 추가")
    public ResponseEntity<Map<String, Object>> addBookmark(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID opportunityId) {
        bookmarkService.add(userDetails.getMember().getId(), opportunityId);
        return ResponseEntity.ok(Map.of("bookmarked", true, "opportunityId", opportunityId.toString()));
    }

    @DeleteMapping("/{opportunityId}")
    @Operation(summary = "북마크 제거")
    public ResponseEntity<Map<String, Object>> removeBookmark(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID opportunityId) {
        bookmarkService.remove(userDetails.getMember().getId(), opportunityId);
        return ResponseEntity.ok(Map.of("bookmarked", false, "opportunityId", opportunityId.toString()));
    }
}
