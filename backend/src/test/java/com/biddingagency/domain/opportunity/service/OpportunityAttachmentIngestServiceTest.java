package com.biddingagency.domain.opportunity.service;

import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.entity.OpportunityAttachment;
import com.biddingagency.domain.opportunity.repository.OpportunityAttachmentRepository;
import com.biddingagency.domain.opportunity.repository.OpportunityRepository;
import com.biddingagency.integration.storage.StorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * TC-PIEE-021 (CR-160): 관리자가 손으로 올린 첨부도 내용 해시를 남긴다(BIZ-080)
 */
@ExtendWith(MockitoExtension.class)
class OpportunityAttachmentIngestServiceTest {

    @Mock
    private OpportunityRepository opportunityRepository;
    @Mock
    private OpportunityAttachmentRepository attachmentRepository;
    @Mock
    private StorageService storageService;
    @InjectMocks
    private OpportunityAttachmentIngestService service;

    // TC-PIEE-021
    @Test
    @DisplayName("손으로올림_내용해시남김")
    void 손으로올림_내용해시남김() throws Exception {
        Opportunity opp = Opportunity.builder().noticeId("N-1").title("t").active(true)
                .firstSeenAt(LocalDateTime.now()).lastModifiedAt(LocalDateTime.now()).build();
        ReflectionTestUtils.setField(opp, "id", UUID.randomUUID());
        given(opportunityRepository.findById(opp.getId())).willReturn(Optional.of(opp));
        given(storageService.store(anyString(), anyString(), any(byte[].class)))
                .willAnswer(inv -> "stored/" + inv.getArgument(1));
        byte[] zip = FakePieeServer.zip(FakePieeServer.files("bid/RFP.pdf", "rfp", "bid/SOW.docx", "sow"));
        MockMultipartFile upload = new MockMultipartFile("files", "W91QVN26QA030.zip", "application/zip", zip);

        service.ingestUploads(opp.getId(), List.of(upload));

        ArgumentCaptor<OpportunityAttachment> saved = ArgumentCaptor.forClass(OpportunityAttachment.class);
        verify(attachmentRepository, times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(OpportunityAttachment::getContentSha256).containsExactly(
                AttachmentFiles.sha256("rfp".getBytes(StandardCharsets.UTF_8)),
                AttachmentFiles.sha256("sow".getBytes(StandardCharsets.UTF_8)));
    }
}
