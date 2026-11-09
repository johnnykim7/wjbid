package com.biddingagency.domain.member.service;

import com.biddingagency.domain.member.AuthError;
import com.biddingagency.domain.member.AuthFlowException;
import com.biddingagency.domain.member.entity.Member;
import com.biddingagency.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final MemberRepository memberRepository;
    private final AuthPolicy authPolicy;
    private final Clock clock;

    @Transactional(readOnly = true)
    public void assertNotLocked(String email) {
        LocalDateTime now = LocalDateTime.now(clock);
        memberRepository.findByEmail(email)
                .filter(member -> member.isLoginLocked(now))
                .ifPresent(member -> {
                    long seconds = Duration.between(now, member.getLoginLockedUntil()).toSeconds();
                    throw new AuthFlowException(AuthError.LOGIN_LOCKED,
                            Map.of("retryAfterMinutes", Math.max(1, (seconds + 59) / 60)));
                });
    }

    @Transactional
    public void recordFailure(String email) {
        LocalDateTime now = LocalDateTime.now(clock);
        memberRepository.findByEmailForUpdate(email)
                .ifPresent(member -> member.recordLoginFailure(
                        authPolicy.loginMaxAttempts(), now.plus(authPolicy.loginLockDuration())));
    }

    @Transactional
    public void clear(String email) {
        memberRepository.findByEmailForUpdate(email).ifPresent(Member::clearLoginFailures);
    }
}
