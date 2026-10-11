package com.biddingagency.controller;

import com.biddingagency.domain.member.service.EmailCodeService;
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
 * 이메일 인증번호 (CR-146, BID-AUTH-005)
 */
@RestController
@RequestMapping("/auth/email-codes")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User authentication and registration")
public class EmailCodeController {

    private final EmailCodeService emailCodeService;

    @PostMapping
    @Operation(summary = "인증번호 보내기")
    public AuthFlowResponses.EmailCodeSent send(@Valid @RequestBody AuthFlowRequests.SendEmailCode request) {
        EmailCodeService.Sent sent = emailCodeService.send(request.email());
        return new AuthFlowResponses.EmailCodeSent(sent.expiresInSeconds(), sent.resendAfterSeconds());
    }

    @PostMapping("/verify")
    @Operation(summary = "인증번호 확인")
    public AuthFlowResponses.EmailCodeVerified verify(@Valid @RequestBody AuthFlowRequests.VerifyEmailCode request) {
        emailCodeService.verify(request.email(), request.code());
        return new AuthFlowResponses.EmailCodeVerified(true);
    }
}
