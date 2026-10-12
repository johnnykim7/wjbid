package com.biddingagency.controller.admin;

import com.biddingagency.domain.member.entity.Member;
import com.biddingagency.domain.member.repository.MemberRepository;
import com.biddingagency.domain.member.service.MembershipService;
import com.biddingagency.security.CustomUserDetails;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin/members")
@RequiredArgsConstructor
@Tag(name = "Admin - Members", description = "Admin member management")
@PreAuthorize("hasRole('ADMIN')")
public class MemberAdminController {

    private final MemberRepository memberRepository;
    private final MembershipService membershipService;

    @GetMapping
    @Operation(summary = "List all members")
    public ResponseEntity<Page<MemberDto>> listMembers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Member> members = memberRepository.findAll(
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        Map<UUID, LocalDate> endsOn = membershipService.currentEndsOn(
                members.getContent().stream().map(Member::getId).toList());
        return ResponseEntity.ok(members.map(m -> MemberDto.from(m, endsOn.get(m.getId()))));
    }

    @GetMapping("/{memberId}/memberships")
    @Operation(summary = "회원 기간 이력 (CR-157)")
    public ResponseEntity<List<MembershipService.Line>> listMemberships(@PathVariable UUID memberId) {
        return ResponseEntity.ok(membershipService.history(memberId));
    }

    @PostMapping("/{memberId}/memberships")
    @Operation(summary = "회원 기간 넣기 (CR-157)")
    public ResponseEntity<MembershipService.Line> grantMembership(
            @PathVariable UUID memberId,
            @Valid @RequestBody GrantMembershipRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        MembershipService.Line saved = membershipService.grant(memberId, request.startsOn(), request.endsOn(),
                request.memo(), userDetails.getMember().getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @DeleteMapping("/{memberId}/memberships/{membershipId}")
    @Operation(summary = "회원 기간 지우기 (CR-157)")
    public ResponseEntity<Void> deleteMembership(@PathVariable UUID memberId, @PathVariable UUID membershipId) {
        membershipService.delete(memberId, membershipId);
        return ResponseEntity.noContent().build();
    }

    public record GrantMembershipRequest(
            LocalDate startsOn,
            LocalDate endsOn,
            @Size(max = 500) String memo
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record MemberDto(
            String id,
            String email,
            String companyName,
            String contactPerson,
            String phone,
            String role,
            String createdAt,
            String lastLoginAt,
            boolean member,
            LocalDate membershipEndsOn
    ) {
        private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        /** membershipEndsOn 은 지금 기간의 끝일. 관리자는 기간 없이 회원이다 (CR-157, BIZ-072) */
        public static MemberDto from(Member m, LocalDate membershipEndsOn) {
            return new MemberDto(
                    m.getId() != null ? m.getId().toString() : null,
                    m.getEmail(),
                    m.getCompanyName(),
                    m.getContactPerson(),
                    m.getPhone(),
                    m.getRole() != null ? m.getRole().name() : null,
                    m.getCreatedAt() != null ? m.getCreatedAt().format(FMT) : null,
                    m.getLastLoginAt() != null ? m.getLastLoginAt().format(FMT) : null,
                    m.isAdmin() || membershipEndsOn != null,
                    membershipEndsOn
            );
        }
    }
}
