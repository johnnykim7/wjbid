package com.biddingagency.common;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 고객 서류 파일 내용의 SHA-256 (CR-169, BIZ-085).
 * 공고별 서류와 공통 서류함 서류가 같은 내용이면 한 번만 읽으려고 올릴 때 남긴다.
 */
public final class ContentHash {

    private static final int BUFFER = 64 * 1024;

    private ContentHash() {
    }

    public static String of(byte[] content) {
        MessageDigest digest = newDigest();
        digest.update(content);
        return HexFormat.of().formatHex(digest.digest());
    }

    public static String of(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            MessageDigest digest = newDigest();
            byte[] buffer = new byte[BUFFER];
            int read;
            while ((read = in.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException e) {
            throw new UncheckedIOException("올린 파일을 읽지 못해 내용 해시를 내지 못함", e);
        }
    }

    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 을 쓸 수 없음", e);
        }
    }
}
