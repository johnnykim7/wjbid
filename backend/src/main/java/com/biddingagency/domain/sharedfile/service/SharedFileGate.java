package com.biddingagency.domain.sharedfile.service;

import com.biddingagency.domain.member.entity.Member;
import com.biddingagency.domain.sharedfile.SharedFileError;
import com.biddingagency.domain.sharedfile.SharedFileException;
import com.biddingagency.domain.sharedfile.entity.SharedFile;
import com.biddingagency.domain.sharedfile.entity.SharedFileLog;
import com.biddingagency.domain.sharedfile.repository.SharedFileLogRepository;
import com.biddingagency.domain.sharedfile.repository.SharedFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 비밀번호 판정과 기록을 한 거래로 묶는다. 틀림과 잠금은 커밋되어야 하므로 예외 대신 결과로 돌려주고,
 * 호출하는 쪽이 거래 밖에서 오류로 바꾼다.
 */
@Service
@RequiredArgsConstructor
public class SharedFileGate {

    public enum Outcome {
        GRANTED, WRONG, LOCKED
    }

    public record Verdict(Outcome outcome, SharedFile file, byte[] key, int remainingAttempts, long retryAfterMinutes) {
    }

    private final SharedFileRepository fileRepository;
    private final SharedFileLogRepository logRepository;
    private final SharedFileCrypto crypto;
    private final SharedFilePolicy policy;
    private final Clock clock;

    @Transactional
    public Verdict verify(UUID id, String password, Member member, String clientIp) {
        SharedFile file = fileRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new SharedFileException(SharedFileError.NOT_FOUND));
        LocalDateTime now = LocalDateTime.now(clock);
        if (file.isLocked(now)) {
            log(file, SharedFileLog.Action.LOCKED, member, clientIp, now);
            return locked(file, now);
        }

        SharedFileCrypto.Keys keys = crypto.derive(password, file.getKeySalt());
        if (SharedFileCrypto.matches(file.getPasswordCheck(), keys.passwordCheck())) {
            file.clearFailures();
            log(file, SharedFileLog.Action.DOWNLOAD, member, clientIp, now);
            return new Verdict(Outcome.GRANTED, file, keys.encryptionKey(), 0, 0);
        }

        int max = policy.maxAttempts();
        log(file, SharedFileLog.Action.WRONG_PASSWORD, member, clientIp, now);
        if (file.recordWrongPassword(max, now.plus(policy.lockDuration()))) {
            log(file, SharedFileLog.Action.LOCKED, member, clientIp, now);
            return locked(file, now);
        }
        return new Verdict(Outcome.WRONG, file, null, max - file.getFailedAttempts(), 0);
    }

    @Transactional
    public void log(SharedFile file, SharedFileLog.Action action, Member member, String clientIp, LocalDateTime now) {
        logRepository.save(SharedFileLog.builder()
                .sharedFileId(file.getId())
                .fileName(file.getFileName())
                .action(action)
                .memberId(member.getId())
                .memberEmail(member.getEmail())
                .clientIp(clientIp)
                .createdAt(now)
                .build());
    }

    private static Verdict locked(SharedFile file, LocalDateTime now) {
        long seconds = Duration.between(now, file.getLockedUntil()).toSeconds();
        long minutes = Math.max(1, (seconds + 59) / 60);
        return new Verdict(Outcome.LOCKED, file, null, 0, minutes);
    }
}
