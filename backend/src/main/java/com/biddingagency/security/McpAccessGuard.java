package com.biddingagency.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

/**
 * Aimbase 가 부르는 /mcp 통로의 열쇠 확인과 내려받기 서명 주소 (CR-144, BIZ-061).
 *
 * Aimbase 의 내려받기 도구(download_file, parse_document url)는 머리글을 싣지 못하고 주소를 그대로 부른다.
 * 그래서 내려받기는 열쇠 대신, 그 경로와 만료 시각에 묶인 서명을 주소에 붙여 건넨다.
 * 서명 열쇠를 따로 두지 않고 MCP 열쇠를 쓰므로 열쇠를 바꾸면 이미 건넨 주소는 열리지 않는다(기한이 짧아 문제 없음).
 */
@Slf4j
@Component
public class McpAccessGuard {

    public static final String KEY_HEADER = "X-API-Key";
    public static final String EXPIRES_PARAM = "exp";
    public static final String SIGNATURE_PARAM = "sig";

    private static final String HMAC = "HmacSHA256";
    private static final String ATTACHMENT_PREFIX = "/mcp/opportunity-attachments/";
    private static final String RFP_FILE_PREFIX = "/mcp/rfp-files/";
    private static final String DOWNLOAD_SUFFIX = "/download";

    private final byte[] apiKey;
    private final Duration downloadUrlTtl;
    private final String selfBaseUrl;
    private final Clock clock;

    public McpAccessGuard(@Value("${app.mcp.api-key:}") String apiKey,
                          @Value("${app.mcp.download-url-ttl:PT1H}") Duration downloadUrlTtl,
                          @Value("${app.self-base-url:http://59.8.160.12:8183/api}") String selfBaseUrl,
                          Clock clock) {
        this.apiKey = apiKey == null ? new byte[0] : apiKey.strip().getBytes(StandardCharsets.UTF_8);
        this.downloadUrlTtl = downloadUrlTtl;
        this.selfBaseUrl = selfBaseUrl;
        this.clock = clock;
        if (this.apiKey.length == 0) {
            log.warn("MCP 열쇠(MCP_API_KEY)가 비어 있어 /mcp 요청을 모두 막는다 (BIZ-061)");
        }
    }

    /** 열쇠 길이나 앞자리로 맞춰 볼 수 없게 같은 시간에 비교한다. */
    public boolean keyMatches(String presented) {
        if (apiKey.length == 0 || presented == null || presented.isEmpty()) {
            return false;
        }
        return MessageDigest.isEqual(apiKey, presented.getBytes(StandardCharsets.UTF_8));
    }

    public String opportunityAttachmentUrl(UUID attachmentId) {
        return signedUrl(ATTACHMENT_PREFIX + attachmentId + DOWNLOAD_SUFFIX);
    }

    public String rfpFileUrl(UUID fileId) {
        return signedUrl(RFP_FILE_PREFIX + fileId + DOWNLOAD_SUFFIX);
    }

    public boolean isDownloadPath(String path) {
        return (path.startsWith(ATTACHMENT_PREFIX) || path.startsWith(RFP_FILE_PREFIX))
                && path.endsWith(DOWNLOAD_SUFFIX);
    }

    public boolean signatureValid(String path, String expires, String signature) {
        if (apiKey.length == 0 || expires == null || signature == null) {
            return false;
        }
        long expiresAt;
        try {
            expiresAt = Long.parseLong(expires);
        } catch (NumberFormatException e) {
            return false;
        }
        if (clock.instant().getEpochSecond() > expiresAt) {
            return false;
        }
        byte[] expected = sign(path, expiresAt).getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(expected, signature.getBytes(StandardCharsets.UTF_8));
    }

    /** 열쇠가 비면 /mcp 가 모두 막히므로 서명 없이 주소만 준다. 워크플로우 입력을 만드는 쪽은 그대로 돈다. */
    private String signedUrl(String path) {
        if (apiKey.length == 0) {
            return selfBaseUrl + path;
        }
        long expiresAt = clock.instant().plus(downloadUrlTtl).getEpochSecond();
        return selfBaseUrl + path + "?" + EXPIRES_PARAM + "=" + expiresAt
                + "&" + SIGNATURE_PARAM + "=" + sign(path, expiresAt);
    }

    private String sign(String path, long expiresAt) {
        try {
            Mac mac = Mac.getInstance(HMAC);
            mac.init(new SecretKeySpec(apiKey, HMAC));
            byte[] raw = mac.doFinal((path + "\n" + expiresAt).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("MCP 내려받기 서명 실패", e);
        }
    }
}
