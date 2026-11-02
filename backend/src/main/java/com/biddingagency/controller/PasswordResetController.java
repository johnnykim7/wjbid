package com.biddingagency.controller;

import com.biddingagency.domain.member.service.MemberService;
import com.biddingagency.dto.AuthFlowRequests;
import com.biddingagency.dto.AuthFlowResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 비밀번호 재설정 (CR-186, BID-AUTH-009). 재설정 인증번호 보내기와 확인은 EmailCodeController 가 한다
 */
@RestController
@RequestMapping("/auth/password-reset")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User authentication and registration")
public class PasswordResetController {

    private final MemberService memberService;

    @PostMapping
    @Operation(summary = "비밀번호 재설정")
    public AuthFlowResponses.PasswordReset reset(@Valid @RequestBody AuthFlowRequests.ResetPassword request) {
        memberService.resetPassword(request.email(), request.newPassword());
        return new AuthFlowResponses.PasswordReset(true);
    }
}
