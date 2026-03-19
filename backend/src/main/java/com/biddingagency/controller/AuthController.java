package com.biddingagency.controller;

import com.biddingagency.domain.member.entity.Member;
import com.biddingagency.domain.member.service.MemberService;
import com.biddingagency.dto.AuthRequest;
import com.biddingagency.dto.AuthResponse;
import com.biddingagency.dto.RegisterRequest;
import com.biddingagency.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication Controller
 *
 * Handles user authentication and registration
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User authentication and registration")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final MemberService memberService;
    private final JwtTokenProvider tokenProvider;

    /**
     * User login
     */
    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticate user and return JWT tokens")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        // Authenticate
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // Generate tokens
        String accessToken = tokenProvider.generateToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(request.getEmail());

        // Update last login
        Member member = memberService.findByEmail(request.getEmail());
        memberService.updateLastLogin(member.getId());

        log.info("Login successful for email: {}", request.getEmail());

        return ResponseEntity.ok(AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getJwtExpiration())
                .email(member.getEmail())
                .roles(java.util.List.of("ROLE_" + member.getRole().name()))
                .build());
    }

    /**
     * User registration
     */
    @PostMapping("/register")
    @Operation(summary = "Register", description = "Register new user account")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration attempt for email: {}", request.getEmail());

        // Register member
        Member member = memberService.register(
                request.getEmail(),
                request.getPassword(),
                request.getCompanyName(),
                request.getContactPerson(),
                request.getPhone()
        );

        // Generate tokens
        String accessToken = tokenProvider.generateToken(member.getEmail());
        String refreshToken = tokenProvider.generateRefreshToken(member.getEmail());

        log.info("Registration successful for email: {}", request.getEmail());

        return ResponseEntity.ok(AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getJwtExpiration())
                .build());
    }

    /**
     * Refresh access token
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Refresh access token using refresh token")
    public ResponseEntity<AuthResponse> refresh(@RequestParam String refreshToken) {
        log.info("Token refresh attempt");

        // Validate refresh token
        if (!tokenProvider.validateToken(refreshToken)) {
            return ResponseEntity.badRequest().build();
        }

        // Extract email from refresh token
        String email = tokenProvider.getEmailFromToken(refreshToken);

        // Generate new access token
        String newAccessToken = tokenProvider.generateToken(email);

        log.info("Token refresh successful for email: {}", email);

        return ResponseEntity.ok(AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken) // Return same refresh token
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getJwtExpiration())
                .build());
    }
}
