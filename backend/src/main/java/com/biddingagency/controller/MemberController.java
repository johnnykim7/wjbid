package com.biddingagency.controller;

import com.biddingagency.domain.member.entity.Member;
import com.biddingagency.domain.member.service.MemberService;
import com.biddingagency.domain.member.service.MembershipService;
import com.biddingagency.security.CustomUserDetails;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;


@RestController
@RequestMapping("/members")
@RequiredArgsConstructor
@Tag(name = "Members", description = "회원 정보 관리")
public class MemberController {

    private final MemberService memberService;
    private final MembershipService membershipService;

    @GetMapping("/me")
    @Operation(summary = "내 프로필 조회")
    public ResponseEntity<MemberDto> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Member member = userDetails.getMember();
        return ResponseEntity.ok(MemberDto.from(member, membershipService.status(member)));
    }

    @PatchMapping("/me")
    @Operation(summary = "내 프로필 수정")
    public ResponseEntity<MemberDto> updateMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {
        Member updated = memberService.updateProfile(
                userDetails.getMember().getId(),
                request.companyName(),
                request.businessRegistrationNumber(),
                request.contactPerson(),
                request.phone(),
                request.address()
        );
        return ResponseEntity.ok(MemberDto.from(updated, membershipService.status(updated)));
    }

    // ── DTOs ─────────────────────────────────────

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record MemberDto(
            String id,
            String email,
            String companyName,
            String businessRegistrationNumber,
            String contactPerson,
            String phone,
            String address,
            String role,
            Membership membership
    ) {
        public static MemberDto from(Member m, MembershipService.Status status) {
            return new MemberDto(
                    m.getId() != null ? m.getId().toString() : null,
                    m.getEmail(),
                    m.getCompanyName(),
                    m.getBusinessRegistrationNumber(),
                    m.getContactPerson(),
                    m.getPhone(),
                    m.getAddress(),
                    m.getRole() != null ? m.getRole().name() : null,
                    new Membership(status.member(), status.endsOn())
            );
        }
    }

    /** 회원 구분 (CR-157). endsOn 은 관리자와 무료회원이면 null 로 싣는다 */
    public record Membership(boolean member, LocalDate endsOn) {}

    public record UpdateProfileRequest(
            @NotBlank String companyName,
            @Size(max = 50) String businessRegistrationNumber,
            String contactPerson,
            String phone,
            String address
    ) {}
}
