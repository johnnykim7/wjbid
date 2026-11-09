package com.biddingagency.domain.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/** 틀림 기록이 롤백되지 않도록 거래 밖에서 인증하고, 기록은 LoginAttemptService 의 거래로 남긴다. */
@Service
@RequiredArgsConstructor
public class LoginService {

    private final AuthenticationManager authenticationManager;
    private final LoginAttemptService loginAttemptService;

    public Authentication authenticate(String email, String password) {
        loginAttemptService.assertNotLocked(email);
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password));
        } catch (BadCredentialsException e) {
            loginAttemptService.recordFailure(email);
            throw e;
        }
        loginAttemptService.clear(email);
        return authentication;
    }
}
