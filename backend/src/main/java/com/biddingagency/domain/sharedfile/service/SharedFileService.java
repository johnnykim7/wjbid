package com.biddingagency.domain.sharedfile.service;

import com.biddingagency.domain.member.entity.Member;
import com.biddingagency.domain.notification.service.MailHtmlValues;
import com.biddingagency.domain.sharedfile.SharedFileError;
import com.biddingagency.domain.sharedfile.SharedFileException;
import com.biddingagency.domain.sharedfile.entity.SharedFile;
import com.biddingagency.domain.sharedfile.entity.SharedFileLog;
import com.biddingagency.domain.sharedfile.repository.SharedFileLogRepository;
import com.biddingagency.domain.sharedfile.repository.SharedFileRepository;
import com.biddingagency.dto.SharedFileResponses;
import com.biddingagency.integration.notification.BpNotificationClient;
import com.biddingagency.integration.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SharedFileService {

    private static final String STORAGE_PREFIX = "shared-files";
    // 저장 경로에 원래 파일 이름이 드러나지 않도록 모든 파일을 같은 이름으로 넘긴다
    private static final String STORED_NAME = "data.enc";
    private static final String MAIL_TEMPLATE = "BID_SHARED_FILE_PASSWORD";
    private static final int MAX_NAME_LENGTH = 255;
    private static final long BYTES_PER_MB = 1024L * 1024L;

    private final SharedFileRepository fileRepository;
    private final SharedFileLogRepository logRepository;
    private final SharedFileGate gate;
    private final SharedFileCrypto crypto;
    private final SharedFilePolicy policy;
    private final StorageService storageService;
    private final BpNotificationClient notificationClient;
    private final Clock clock;

    public record Download(String fileName, long sizeBytes, String storagePath, byte[] key) {
    }

    public SharedFileResponses.Uploaded upload(MultipartFile file, Member uploader, String clientIp) {
        String fileName = cleanName(file.getOriginalFilename());
        validate(file, fileName);

        String password = crypto.newPassword();
        byte[] salt = crypto.newSalt();
        SharedFileCrypto.Keys keys = crypto.derive(password, salt);
        String storagePath = storeEncrypted(file, keys.encryptionKey());

        SharedFile saved;
        try {
            saved = fileRepository.save(SharedFile.builder()
                    .fileName(fileName)
                    .sizeBytes(file.getSize())
                    .storagePath(storagePath)
                    .keySalt(salt)
                    .passwordCheck(keys.passwordCheck())
                    .uploadedById(uploader.getId())
                    .uploadedByEmail(uploader.getEmail())
                    .build());
        } catch (RuntimeException e) {
            storageService.delete(storagePath);
            throw e;
        }

        if (!mailPassword(uploader.getEmail(), fileName, password)) {
            fileRepository.delete(saved);
            storageService.delete(storagePath);
            throw new SharedFileException(SharedFileError.MAIL_FAILED);
        }

        gate.log(saved, SharedFileLog.Action.UPLOAD, uploader, clientIp, LocalDateTime.now(clock));
        return new SharedFileResponses.Uploaded(saved.getId(), saved.getFileName(), saved.getSizeBytes(),
                saved.getCreatedAt(), maskEmail(uploader.getEmail()));
    }

    public SharedFileResponses.ListView list(int page, int size, Member viewer) {
        Page<SharedFile> files = fileRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
        return new SharedFileResponses.ListView(
                new SharedFileResponses.Rules(policy.maxUploadMb(), policy.blockedExtensions()),
                files.map(f -> SharedFileResponses.Item.of(f, viewer.getId(), LocalDateTime.now(clock))).getContent(),
                files.getNumber(), files.getTotalPages(), files.getTotalElements());
    }

    /** 비밀번호가 맞을 때만 돌려준다. 틀림과 잠금은 거래가 끝난 뒤에 오류로 바꾼다. */
    public Download authorizeDownload(UUID id, String password, Member member, String clientIp) {
        SharedFileGate.Verdict verdict = gate.verify(id, password, member, clientIp);
        switch (verdict.outcome()) {
            case WRONG -> throw new SharedFileException(SharedFileError.WRONG_PASSWORD,
                    Map.of("remainingAttempts", verdict.remainingAttempts()));
            case LOCKED -> throw new SharedFileException(SharedFileError.LOCKED,
                    Map.of("retryAfterMinutes", verdict.retryAfterMinutes()));
            default -> {
                return new Download(verdict.file().getFileName(), verdict.file().getSizeBytes(),
                        verdict.file().getStoragePath(), verdict.key());
            }
        }
    }

    public void writeDecrypted(Download download, OutputStream out) throws IOException {
        try (InputStream cipher = storageService.open(download.storagePath())) {
            crypto.decrypt(cipher, out, download.key());
        }
    }

    public void delete(UUID id, Member member, String clientIp) {
        SharedFile file = fileRepository.findById(id)
                .orElseThrow(() -> new SharedFileException(SharedFileError.NOT_FOUND));
        if (!file.getUploadedById().equals(member.getId())) {
            throw new SharedFileException(SharedFileError.NOT_OWNER);
        }
        gate.log(file, SharedFileLog.Action.DELETE, member, clientIp, LocalDateTime.now(clock));
        fileRepository.delete(file);
        storageService.delete(file.getStoragePath());
    }

    public SharedFileResponses.LogPage logs(int page, int size) {
        Page<SharedFileLog> logs = logRepository.findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(page, size));
        return new SharedFileResponses.LogPage(
                logs.map(SharedFileResponses.LogItem::of).getContent(),
                logs.getNumber(), logs.getTotalPages(), logs.getTotalElements());
    }

    private void validate(MultipartFile file, String fileName) {
        if (file.isEmpty()) {
            throw new SharedFileException(SharedFileError.EMPTY_FILE);
        }
        if (policy.blockedExtensions().contains(extensionOf(fileName))) {
            throw new SharedFileException(SharedFileError.BLOCKED_EXTENSION);
        }
        if (file.getSize() > policy.maxUploadMb() * BYTES_PER_MB) {
            throw new SharedFileException(SharedFileError.TOO_LARGE);
        }
    }

    private String storeEncrypted(MultipartFile file, byte[] key) {
        try (InputStream plain = file.getInputStream();
             InputStream encrypted = crypto.encrypting(plain, key)) {
            return storageService.store(STORAGE_PREFIX, STORED_NAME, encrypted);
        } catch (IOException e) {
            throw new UncheckedIOException("자료실 파일 저장 실패", e);
        }
    }

    private boolean mailPassword(String email, String fileName, String password) {
        try {
            return notificationClient.sendEmail(email, MAIL_TEMPLATE,
                    MailHtmlValues.escape(Map.of("fileName", fileName, "password", password))).success();
        } catch (RuntimeException e) {
            log.error("자료실 비밀번호 메일 발송 오류: type={}", e.getClass().getSimpleName());
            return false;
        }
    }

    private static String cleanName(String original) {
        if (original == null) {
            throw new SharedFileException(SharedFileError.INVALID_NAME);
        }
        String name = original.substring(Math.max(original.lastIndexOf('/'), original.lastIndexOf('\\')) + 1)
                .replaceAll("\\p{Cntrl}", "")
                .trim();
        if (name.isEmpty()) {
            throw new SharedFileException(SharedFileError.INVALID_NAME);
        }
        return name.length() <= MAX_NAME_LENGTH ? name : name.substring(name.length() - MAX_NAME_LENGTH);
    }

    // Windows 는 끝의 점과 공백을 버리고 실행하므로 "a.exe." 도 exe 로 본다
    private static String extensionOf(String fileName) {
        String trimmed = fileName.replaceAll("[. ]+$", "");
        int dot = trimmed.lastIndexOf('.');
        return dot < 0 ? "" : trimmed.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        return email.substring(0, Math.min(2, at)) + "***" + email.substring(at);
    }
}
