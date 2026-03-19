package com.biddingagency.controller.admin;

import com.biddingagency.domain.member.entity.Member;
import com.biddingagency.domain.member.repository.MemberRepository;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/admin/members")
@RequiredArgsConstructor
@Tag(name = "Admin - Members", description = "Admin member management")
@PreAuthorize("hasRole('ADMIN')")
public class MemberAdminController {

    private final MemberRepository memberRepository;

    @GetMapping
    @Operation(summary = "List all members")
    public ResponseEntity<Page<MemberDto>> listMembers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Member> members = memberRepository.findAll(
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(members.map(MemberDto::from));
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record MemberDto(
            String id,
            String email,
            String companyName,
            String contactPerson,
            String phone,
            String role,
            String createdAt,
            String lastLoginAt
    ) {
        private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        public static MemberDto from(Member m) {
            return new MemberDto(
                    m.getId() != null ? m.getId().toString() : null,
                    m.getEmail(),
                    m.getCompanyName(),
                    m.getContactPerson(),
                    m.getPhone(),
                    m.getRole() != null ? m.getRole().name() : null,
                    m.getCreatedAt() != null ? m.getCreatedAt().format(FMT) : null,
                    m.getLastLoginAt() != null ? m.getLastLoginAt().format(FMT) : null
            );
        }
    }
}
