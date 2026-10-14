package com.biddingagency.domain.opportunity.service;

import com.biddingagency.domain.opportunity.entity.AttachmentDownloadStatus;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.entity.OpportunityAttachment;
import com.biddingagency.domain.opportunity.repository.OpportunityAttachmentRepository;
import com.biddingagency.integration.samgov.quota.SamQuotaLogger;
import com.biddingagency.integration.storage.StorageService;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

/**
 * TC-PIEE-020 (CR-160): SAM 첨부 자동 받기가 저장한 파일의 내용 해시를 남긴다(BIZ-080)
 */
@ExtendWith(MockitoExtension.class)
class AttachmentAutoDownloadServiceTest {

    @Mock
    private OpportunityAttachmentRepository attachmentRepository;
    @Mock
    private StorageService storageService;
    @Mock
    private SamQuotaLogger quotaLogger;
    @Mock
    private CloseableHttpClient httpClient;

    // TC-PIEE-020
    @Test
    @DisplayName("SAM자동받기_내용해시남김")
    @SuppressWarnings("unchecked")
    void SAM자동받기_내용해시남김() throws Exception {
        AttachmentAutoDownloadService service = new AttachmentAutoDownloadService(
                attachmentRepository, storageService, "test-key", quotaLogger);
        ReflectionTestUtils.setField(service, "httpClient", httpClient);
        Opportunity opp = Opportunity.builder().noticeId("N-1").title("t").active(true)
                .firstSeenAt(LocalDateTime.now()).lastModifiedAt(LocalDateTime.now()).build();
        ReflectionTestUtils.setField(opp, "id", UUID.randomUUID());
        OpportunityAttachment attachment = OpportunityAttachment.builder().opportunity(opp).fileName("download")
                .sourceUrl("https://sam.gov/api/prod/opps/v3/opportunities/resources/files/abc/download")
                .downloadStatus(AttachmentDownloadStatus.PENDING).build();
        UUID attachmentId = UUID.randomUUID();
        given(attachmentRepository.findById(attachmentId)).willReturn(Optional.of(attachment));
        byte[] body = "rfp body".getBytes(StandardCharsets.UTF_8);
        given(httpClient.execute(any(ClassicHttpRequest.class), any(HttpClientResponseHandler.class)))
                .willAnswer(inv -> {
                    BasicClassicHttpResponse response = new BasicClassicHttpResponse(200);
                    response.setEntity(new ByteArrayEntity(body, ContentType.APPLICATION_PDF));
                    return ((HttpClientResponseHandler<Object>) inv.getArgument(1)).handleResponse(response);
                });
        given(storageService.store(anyString(), anyString(), any(byte[].class))).willReturn("stored/rfp.pdf");

        service.tryAutoFetch(attachmentId);

        assertThat(attachment.getContentSha256()).isEqualTo(AttachmentFiles.sha256(body));
    }
}
