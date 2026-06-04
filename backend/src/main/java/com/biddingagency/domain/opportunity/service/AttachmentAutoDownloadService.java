package com.biddingagency.domain.opportunity.service;

import com.biddingagency.domain.opportunity.entity.OpportunityAttachment;
import com.biddingagency.domain.opportunity.repository.OpportunityAttachmentRepository;
import com.biddingagency.integration.samgov.quota.SamQuotaLogger;
import com.biddingagency.integration.storage.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * CR-025: SAM 자체호스팅 첨부 자동 다운로드.
 *
 * <p>SAM resourceLinks 중 화이트리스트(아래 정규식)에 매칭되는 URL은 SAM API 키로 직접 다운로드해
 * StorageService에 적재한다. 매칭되지 않는 외부 도메인(DLA/PIEE 등 로그인·캡차 필요)은 자동 시도하지 않고
 * MANUAL_FETCH_REQUIRED 표식을 유지(=관리자 수동 업로드 대상)한다.
 *
 * <p>다운로드 흐름: SAM resources/.../download?api_key= → 302 → S3 presigned → 파일 바이트.
 * Apache HC5 기본 redirect 처리로 302를 따라간다.
 */
@Slf4j
@Service
public class AttachmentAutoDownloadService {

    /**
     * 자동 다운로드 화이트리스트.
     * 예: https://sam.gov/api/prod/opps/v3/opportunities/resources/files/{id}/download
     *     https://api.sam.gov/prod/opps/.../resources/.../download?...
     */
    private static final Pattern SAM_DOWNLOAD_WHITELIST = Pattern.compile(
            "^https?://(api\\.)?sam\\.gov/.*/resources/.*/download(\\?.*)?$");

    private static final long MAX_FILE_BYTES = 100L * 1024 * 1024; // 100MB

    private final OpportunityAttachmentRepository attachmentRepository;
    private final StorageService storageService;
    private final String samApiKey;
    private final CloseableHttpClient httpClient;
    private final com.biddingagency.integration.samgov.quota.SamQuotaLogger quotaLogger;

    public AttachmentAutoDownloadService(
            OpportunityAttachmentRepository attachmentRepository,
            StorageService storageService,
            @Value("${app.sam-gov.api-key}") String samApiKey,
            com.biddingagency.integration.samgov.quota.SamQuotaLogger quotaLogger) {
        this.attachmentRepository = attachmentRepository;
        this.storageService = storageService;
        this.samApiKey = samApiKey;
        this.quotaLogger = quotaLogger;
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofSeconds(5))   // connect
                .setResponseTimeout(Timeout.ofSeconds(60))           // read
                .build();
        this.httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();
    }

    /** 화이트리스트 매칭 여부 — ingestResourceLinks에서 PENDING/MANUAL 분기 판단에 사용 */
    public boolean isAutoDownloadable(String url) {
        return url != null && SAM_DOWNLOAD_WHITELIST.matcher(url).matches();
    }

    /**
     * 비동기 자동 다운로드 트리거. 신규 수집 시 PENDING으로 적재한 첨부를 별도 풀에서 처리.
     */
    @Async("attachmentDownloadExecutor")
    public void tryAutoFetchAsync(UUID attachmentId) {
        try {
            tryAutoFetch(attachmentId);
        } catch (Exception e) {
            log.warn("[CR-025] 자동 다운로드 비동기 처리 실패: attachmentId={}", attachmentId, e);
        }
    }

    /**
     * 동기 자동 다운로드. 관리자 "자동으로 다운 시도" 버튼에서도 호출(결과 즉시 응답).
     * 화이트리스트 비매칭이면 시도하지 않고 false 반환.
     *
     * @return 성공 시 true, 실패/스킵 시 false
     */
    @Transactional
    public boolean tryAutoFetch(UUID attachmentId) {
        OpportunityAttachment attachment = attachmentRepository.findById(attachmentId).orElse(null);
        if (attachment == null) {
            log.warn("[CR-025] 자동 다운로드 대상 첨부 없음: {}", attachmentId);
            return false;
        }
        String url = attachment.getSourceUrl();
        if (!isAutoDownloadable(url)) {
            log.info("[CR-025] 화이트리스트 비매칭 — 자동 다운로드 스킵: {}", url);
            return false;
        }

        String downloadUrl = appendApiKey(url);
        try {
            HttpGet request = new HttpGet(downloadUrl);
            request.addHeader("Accept", "*/*");

            return httpClient.execute(request, response -> {
                int status = response.getCode();
                org.apache.hc.core5.http.Header[] respHeaders = response.getHeaders();
                // CR-036: 첨부 다운로드도 api.sam.gov에 api_key로 호출 — SAM 쿼터 계측
                quotaLogger.record(SamQuotaLogger.EP_ATTACHMENT, status, respHeaders,
                        status >= 200 && status < 300, null);
                if (status >= 200 && status < 300) {
                    byte[] body = EntityUtils.toByteArray(response.getEntity());
                    if (body == null || body.length == 0) {
                        attachment.markFailed("빈 응답(0 bytes)");
                        attachmentRepository.save(attachment);
                        return false;
                    }
                    if (body.length > MAX_FILE_BYTES) {
                        attachment.markFailed("파일 크기 초과(" + body.length + " bytes > 100MB)");
                        attachmentRepository.save(attachment);
                        return false;
                    }
                    String fileName = resolveFileName(response.getHeaders("Content-Disposition"),
                            attachment.getFileName());
                    String contentType = headerValue(response.getHeaders("Content-Type"));
                    String storageUrl = storageService.store(
                            "opportunity-attachments/" + attachment.getOpportunity().getId(),
                            fileName, body);
                    attachment.applyAutoDownloadSuccess(fileName, (long) body.length, contentType, storageUrl);
                    attachmentRepository.save(attachment);
                    log.info("[CR-025] 자동 다운로드 성공: attachmentId={}, fileName={}, {} bytes",
                            attachmentId, fileName, body.length);
                    return true;
                } else {
                    // 4xx/5xx 모두 사유 기록 후 FAILED. (5xx 재시도는 후속 — MVP는 단발)
                    attachment.markFailed("HTTP " + status);
                    attachmentRepository.save(attachment);
                    log.warn("[CR-025] 자동 다운로드 실패: attachmentId={}, status={}", attachmentId, status);
                    return false;
                }
            });
        } catch (Exception e) {
            // CR-036: 응답을 못 받은 호출(timeout/IO 실패)도 쿼터를 소진했을 수 있다 — error로 계측
            quotaLogger.record(SamQuotaLogger.EP_ATTACHMENT, null, null, false,
                    e.getClass().getSimpleName() + ": " + e.getMessage());
            attachment.markFailed(truncate(e.getClass().getSimpleName() + ": " + e.getMessage()));
            attachmentRepository.save(attachment);
            log.warn("[CR-025] 자동 다운로드 예외: attachmentId={}", attachmentId, e);
            return false;
        }
    }

    /** url에 api_key 쿼리 파라미터를 안전하게 부착 */
    private String appendApiKey(String url) {
        String sep = url.contains("?") ? "&" : "?";
        return url + sep + "api_key=" + samApiKey;
    }

    /** Content-Disposition 헤더에서 filename 추출, 없으면 기존 파일명 유지 */
    private String resolveFileName(Header[] dispositions, String fallback) {
        if (dispositions != null) {
            for (Header h : dispositions) {
                String v = h.getValue();
                if (v == null) continue;
                int idx = v.toLowerCase().indexOf("filename=");
                if (idx >= 0) {
                    String name = v.substring(idx + "filename=".length()).trim();
                    if (name.startsWith("\"") && name.endsWith("\"") && name.length() >= 2) {
                        name = name.substring(1, name.length() - 1);
                    }
                    if (!name.isBlank()) {
                        return name.length() > 500 ? name.substring(0, 500) : name;
                    }
                }
            }
        }
        return fallback;
    }

    private String headerValue(Header[] headers) {
        return (headers != null && headers.length > 0) ? headers[0].getValue() : null;
    }

    private String truncate(String s) {
        if (s == null) return null;
        return s.length() > 500 ? s.substring(0, 500) : s;
    }
}
