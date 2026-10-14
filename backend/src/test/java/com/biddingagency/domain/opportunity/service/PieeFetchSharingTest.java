package com.biddingagency.domain.opportunity.service;

import com.biddingagency.config.JpaConfig;
import com.biddingagency.domain.opportunity.entity.AttachmentDownloadStatus;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.entity.OpportunityAttachment;
import com.biddingagency.domain.opportunity.repository.OpportunityAttachmentRepository;
import com.biddingagency.domain.opportunity.repository.OpportunityRepository;
import com.biddingagency.integration.piee.PieeClient;
import com.biddingagency.integration.storage.StorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * TC-PIEE-008 (CR-160): 같은 공고번호 앞 판에 같은 내용이 손으로 올린 첨부로만 있으면 그 저장 파일을 같이 쓰지 않고 새로 저장한다(BIZ-080).
 * 같이 쓸 저장 파일을 고르는 조회를 실제 DB 로 본다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
class PieeFetchSharingTest {

    private static final String SOL_NO = "W91QVN26QA030";
    private static final LocalDateTime AT = LocalDateTime.of(2026, 10, 14, 13, 0);

    @Autowired
    private OpportunityRepository opportunityRepository;
    @Autowired
    private OpportunityAttachmentRepository attachmentRepository;

    private final StorageService storageService = mock(StorageService.class);
    private final PieeSettings settings = mock(PieeSettings.class);
    private FakePieeServer piee;

    @BeforeEach
    void setUp() throws Exception {
        piee = new FakePieeServer();
        given(settings.noticeUrl(SOL_NO)).willReturn(Optional.of(piee.noticeUrl(SOL_NO)));
        given(settings.maxZipBytes()).willReturn(10L * 1024 * 1024);
        given(settings.timeout()).willReturn(Duration.ofSeconds(30));
        given(settings.now()).willReturn(AT);
        given(storageService.store(anyString(), anyString(), any(InputStream.class)))
                .willAnswer(inv -> "stored/new/" + inv.getArgument(1));
    }

    @AfterEach
    void tearDown() {
        piee.close();
    }

    private Opportunity opportunity(String noticeId) {
        return opportunityRepository.save(Opportunity.builder().noticeId(noticeId).solicitationNumber(SOL_NO)
                .title("t").type("Solicitation").active(true).firstSeenAt(AT).lastModifiedAt(AT).build());
    }

    // TC-PIEE-008
    @Test
    @DisplayName("다른판손으로올린첨부만같음_새로저장")
    void 다른판손으로올린첨부만같음_새로저장() throws Exception {
        Opportunity previous = opportunity("N-AMD-0");
        String rfpHash = AttachmentFiles.sha256("rfp".getBytes(StandardCharsets.UTF_8));
        attachmentRepository.save(OpportunityAttachment.builder().opportunity(previous).fileName("RFP.pdf")
                .sourceUrl(AttachmentFiles.ADMIN_UPLOAD_SOURCE).storageUrl("stored/admin/RFP.pdf")
                .downloadStatus(AttachmentDownloadStatus.SUCCESS).contentSha256(rfpHash).build());
        Opportunity amendment = opportunity("N-AMD-1");
        piee.postBody = FakePieeServer.zip(FakePieeServer.files("RFP.pdf", "rfp"));
        PieeFetchService service = new PieeFetchService(opportunityRepository, attachmentRepository,
                storageService, new PieeClient(), settings);

        service.fetch(amendment.getId());

        verify(storageService).store(eq(AttachmentFiles.storagePrefix(amendment.getId())), eq("RFP.pdf"),
                any(InputStream.class));
        assertThat(attachmentRepository.findByOpportunityId(amendment.getId()))
                .singleElement().extracting(OpportunityAttachment::getStorageUrl).isEqualTo("stored/new/RFP.pdf");
    }
}
