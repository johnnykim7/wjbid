package com.biddingagency.controller;

import com.biddingagency.domain.federal.dto.FederalContractResponses.LookupResponse;
import com.biddingagency.domain.federal.dto.FederalContractResponses.MemberContractRow;
import com.biddingagency.domain.federal.dto.FederalContractResponses.MemberContractsResponse;
import com.biddingagency.domain.federal.dto.MemberContractRequest;
import com.biddingagency.domain.federal.service.MemberContractService;
import com.biddingagency.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 옵션 연장 알림의 회원 계약 (CR-142, T3-2 C-5). 로그인 회원 본인 계약만, 남의 계약 id 는 404.
 */
@RestController
@RequestMapping("/member-contracts")
@RequiredArgsConstructor
@Tag(name = "MemberContract", description = "옵션 연장 알림 회원 계약 (CR-142)")
public class MemberContractController {

    private final MemberContractService memberContractService;

    @GetMapping
    @Operation(summary = "내 계약 목록과 알림 설정")
    public ResponseEntity<MemberContractsResponse> list(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(memberContractService.list(userDetails.getMember().getId()));
    }

    @GetMapping("/lookup")
    @Operation(summary = "계약번호로 찾기", description = "표에 없으면 USAspending 검색 1회, 실패하면 502")
    public ResponseEntity<LookupResponse> lookup(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                 @RequestParam(required = false) String piid) {
        return ResponseEntity.ok(memberContractService.lookup(userDetails.getMember().getId(), piid));
    }

    @PostMapping
    @Operation(summary = "등록", description = "찾은 계약은 공개 자료로, 못 찾으면 직접 넣은 날짜로")
    public ResponseEntity<MemberContractRow> register(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                      @RequestBody MemberContractRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(memberContractService.register(userDetails.getMember().getId(), request));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "직접 넣은 계약 고치기")
    public ResponseEntity<MemberContractRow> update(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                    @PathVariable UUID id,
                                                    @RequestBody MemberContractRequest request) {
        return ResponseEntity.ok(memberContractService.update(userDetails.getMember().getId(), id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "지우기")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable UUID id) {
        memberContractService.delete(userDetails.getMember().getId(), id);
        return ResponseEntity.noContent().build();
    }
}
