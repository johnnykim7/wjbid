package com.biddingagency.integration.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 로컬 디스크 스토리지 (CR-013).
 * base-dir 아래 {keyPrefix}/{uuid}_{원본파일명}으로 저장. storageUrl = base-dir 기준 상대경로.
 */
@Slf4j
@Service
public class LocalFileStorageService implements StorageService {

    private final Path baseDir;

    public LocalFileStorageService(@Value("${app.storage.local.base-dir:./storage/rfp-samples}") String baseDir) {
        this.baseDir = Paths.get(baseDir).toAbsolutePath().normalize();
    }

    @Override
    public String store(String keyPrefix, MultipartFile file) {
        String original = sanitize(file.getOriginalFilename());
        String relativePath = keyPrefix + "/" + UUID.randomUUID() + "_" + original;
        Path target = resolveWithin(relativePath);
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
            log.info("[storage] 파일 저장: {} ({} bytes)", relativePath, file.getSize());
            return relativePath;
        } catch (IOException e) {
            throw new UncheckedIOException("파일 저장 실패: " + relativePath, e);
        }
    }

    @Override
    public byte[] load(String storageUrl) {
        try {
            return Files.readAllBytes(resolveWithin(storageUrl));
        } catch (IOException e) {
            throw new UncheckedIOException("파일 로드 실패: " + storageUrl, e);
        }
    }

    @Override
    public void delete(String storageUrl) {
        try {
            Files.deleteIfExists(resolveWithin(storageUrl));
        } catch (IOException e) {
            log.warn("[storage] 파일 삭제 실패: {}", storageUrl, e);
        }
    }

    /** path traversal 방지: 항상 baseDir 내부로 한정 */
    private Path resolveWithin(String relativePath) {
        Path resolved = baseDir.resolve(relativePath).normalize();
        if (!resolved.startsWith(baseDir)) {
            throw new IllegalArgumentException("허용되지 않은 경로: " + relativePath);
        }
        return resolved;
    }

    private String sanitize(String name) {
        if (name == null || name.isBlank()) return "file";
        return name.replaceAll("[/\\\\]", "_");
    }
}
