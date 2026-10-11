package com.biddingagency.controller;

import com.biddingagency.domain.member.entity.Member;
import com.biddingagency.domain.member.service.SnsAuthService;
import com.biddingagency.dto.AuthFlowRequests;
import com.biddingagency.dto.AuthFlowResponses;
import com.biddingagency.dto.AuthResponse;
import com.biddingagency.security.JwtTokenProvider;
import com.biddingagency.security.sns.SnsClientRegistrations;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * SNS 가입과 로그인 (CR-146, BID-AUTH-006, BID-AUTH-007).
 * SNS 왕복 자체(/oauth2/authorization, /login/oauth2/code)는 스프링 보안 필터와 SnsLoginHandlers 가 맡는다.
 */
@RestController
@RequestMapping("/auth/sns")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User authentication and registration")
public class SnsAuthController {

    private final SnsAuthService snsAuthService;
    private final SnsClientRegistrations registrations;
    private final JwtTokenProvider tokenProvider;

    @GetMapping("/providers")
    @Operation(summary = "키가 설정된 SNS 목록")
    public AuthFlowResponses.SnsProviders providers() {
        return new AuthFlowResponses.SnsProviders(registrations.configuredProviders().stream()
                .map(p -> new AuthFlowResponses.SnsProviderItem(p.name(),
                        OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI + "/" + p.registrationId()))
                .toList());
    }

    @PostMapping("/login")
    @Operation(summary = "SNS 로그인 표를 토큰으로 바꾸기")
    public AuthResponse login(@Valid @RequestBody AuthFlowRequests.SnsLogin request) {
        return tokens(snsAuthService.login(request.ticket()));
    }

    @GetMapping("/signup/{ticket}")
    @Operation(summary = "SNS 가입 화면 채울 값")
    public AuthFlowResponses.SnsSignupInfo signupInfo(@PathVariable String ticket) {
        SnsAuthService.SignupInfo info = snsAuthService.signupInfo(ticket);
        return new AuthFlowResponses.SnsSignupInfo(info.provider().name(), info.email(), info.emailVerifiedByProvider());
    }

    @PostMapping("/register")
    @Operation(summary = "SNS 가입")
    public AuthResponse register(@Valid @RequestBody AuthFlowRequests.SnsRegister request) {
        Member member = snsAuthService.register(new SnsAuthService.RegisterCommand(
                request.ticket(), request.email(), request.companyName().trim(),
                request.businessRegistrationNumber().trim(), request.contactPerson().trim(), request.phone().trim(),
                request.agreeTerms(), request.agreePrivacy(), request.agreeMarketing()));
        return tokens(member);
    }

    private AuthResponse tokens(Member member) {
        return AuthResponse.builder()
                .accessToken(tokenProvider.generateToken(member.getEmail()))
                .refreshToken(tokenProvider.generateRefreshToken(member.getEmail()))
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getJwtExpiration())
                .email(member.getEmail())
                .roles(List.of("ROLE_" + member.getRole().name()))
                .build();
    }
}
