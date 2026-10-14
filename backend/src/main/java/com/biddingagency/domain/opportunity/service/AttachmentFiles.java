package com.biddingagency.domain.opportunity.service;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** 공고 첨부를 저장할 때 쓰는 이름, 형식, 내용 해시(CR-037, CR-160) */
public final class AttachmentFiles {

    public static final String STORAGE_PREFIX = "opportunity-attachments/";
    public static final String ADMIN_UPLOAD_SOURCE = "admin-upload";
    private static final int HASH_BUFFER = 64 * 1024;

    private AttachmentFiles() {
    }

    public static String storagePrefix(java.util.UUID opportunityId) {
        return STORAGE_PREFIX + opportunityId;
    }

    /** 확장자 또는 content-type 으로 압축인지 본다 */
    public static boolean isZip(String fileName, String contentType) {
        if (fileName != null && fileName.toLowerCase().endsWith(".zip")) return true;
        return contentType != null
                && (contentType.equals("application/zip")
                || contentType.equals("application/x-zip-compressed"));
    }

    /** 경로의 마지막 이름만 쓴다. 압축 안 경로로 저장 폴더 밖에 쓰는 것(zip-slip)도 이걸로 막는다 */
    public static String flatten(String name) {
        if (name == null) return null;
        String normalized = name.replace('\\', '/');
        int idx = normalized.lastIndexOf('/');
        return idx >= 0 ? normalized.substring(idx + 1) : normalized;
    }

    /** 압축 안 파일은 content-type 이 따라오지 않아 확장자로 정한다 */
    public static String guessContentType(String fileName) {
        if (fileName == null) return "application/octet-stream";
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".doc")) return "application/msword";
        if (lower.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (lower.endsWith(".xls")) return "application/vnd.ms-excel";
        if (lower.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        if (lower.endsWith(".txt")) return "text/plain";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        return "application/octet-stream";
    }

    public static String sha256(byte[] content) {
        MessageDigest digest = newDigest();
        digest.update(content);
        return HexFormat.of().formatHex(digest.digest());
    }

    /** 흐름을 끝까지 읽어 해시와 읽은 바이트 수를 낸다. 흐름은 닫지 않는다 */
    public static Digest sha256(InputStream content) throws IOException {
        MessageDigest digest = newDigest();
        byte[] buffer = new byte[HASH_BUFFER];
        long size = 0;
        int read;
        while ((read = content.read(buffer)) != -1) {
            digest.update(buffer, 0, read);
            size += read;
        }
        return new Digest(HexFormat.of().formatHex(digest.digest()), size);
    }

    public record Digest(String sha256, long size) {
    }

    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 을 쓸 수 없음", e);
        }
    }
}
