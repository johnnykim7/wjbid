package com.biddingagency.domain.opportunity.service;

import com.biddingagency.domain.opportunity.entity.AttachmentDownloadStatus;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.entity.OpportunityAttachment;
import com.biddingagency.domain.opportunity.entity.PieeFetchStatus;
import com.biddingagency.domain.opportunity.repository.OpportunityAttachmentRepository;
import com.biddingagency.domain.opportunity.repository.OpportunityRepository;
import com.biddingagency.integration.piee.PieeClient;
import com.biddingagency.integration.storage.StorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * TC-PIEE-001 ~ 007, 009, 010 (CR-160): PIEE 입찰서류 받기. PIEE 는 흉내 서버로 두고 실제 PieeClient 로 부른다.
 * TC-PIEE-008 은 같이 쓸 저장 파일을 고르는 조회를 실제 DB 로 봐야 해서 PieeFetchSharingTest 에 있다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PieeFetchServiceTest {

    private static final String SOL_NO = "W91QVN26QA030";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 14, 13, 0);

    @Mock
    private OpportunityRepository opportunityRepository;
    @Mock
    private OpportunityAttachmentRepository attachmentRepository;
    @Mock
    private StorageService storageService;
    @Mock
    private PieeSettings settings;

    private FakePieeServer piee;
    private PieeFetchService service;
    private Opportunity opp;
    private final List<OpportunityAttachment> existing = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        piee = new FakePieeServer();
        service = new PieeFetchService(opportunityRepository, attachmentRepository, storageService,
                new PieeClient(), settings);
        opp = Opportunity.builder().noticeId("N-1").solicitationNumber(SOL_NO).title("t").active(true)
                .firstSeenAt(NOW).lastModifiedAt(NOW).build();
        ReflectionTestUtils.setField(opp, "id", UUID.randomUUID());
        given(opportunityRepository.findById(opp.getId())).willReturn(Optional.of(opp));
        given(opportunityRepository.finishPieeFetch(any(), any(), any(), any(), any(), any())).willReturn(1);
        given(attachmentRepository.findByOpportunityId(opp.getId())).willReturn(existing);
        given(attachmentRepository.save(any(OpportunityAttachment.class))).willAnswer(inv -> inv.getArgument(0));
        given(storageService.store(anyString(), anyString(), any(InputStream.class)))
                .willAnswer(inv -> "stored/" + inv.getArgument(1));
        given(settings.noticeUrl(SOL_NO)).willReturn(Optional.of(piee.noticeUrl(SOL_NO)));
        given(settings.maxZipBytes()).willReturn(10L * 1024 * 1024);
        given(settings.timeout()).willReturn(Duration.ofSeconds(30));
        given(settings.now()).willReturn(NOW);
    }

    @AfterEach
    void tearDown() {
        piee.close();
    }

    private List<OpportunityAttachment> savedRows() {
        ArgumentCaptor<OpportunityAttachment> captor = ArgumentCaptor.forClass(OpportunityAttachment.class);
        verify(attachmentRepository, atLeastOnce()).save(captor.capture());
        return captor.getAllValues();
    }

    private void verifyFinished(PieeFetchStatus status, Integer fileCount) {
        verify(opportunityRepository).finishPieeFetch(eq(opp.getId()), eq(PieeFetchStatus.FETCHING), eq(status),
                eq(NOW), fileCount == null ? isNull() : eq(fileCount), isNull());
    }

    private static String sha(String text) {
        return AttachmentFiles.sha256(text.getBytes(StandardCharsets.UTF_8));
    }

    // TC-PIEE-001
    @Test
    @DisplayName("새공고_PIEE에있음_파일마다첨부로넣음")
    void 새공고_PIEE에있음_파일마다첨부로넣음() throws Exception {
        piee.postBody = FakePieeServer.zip(FakePieeServer.files(
                "W91QVN26QA030/RFP.pdf", "rfp", "W91QVN26QA030/SOW.docx", "sow", "Wage.pdf", "wage"));

        service.fetch(opp.getId());

        List<OpportunityAttachment> rows = savedRows();
        assertThat(rows).extracting(OpportunityAttachment::getFileName).containsExactly("RFP.pdf", "SOW.docx", "Wage.pdf");
        assertThat(rows).allSatisfy(a -> {
            assertThat(a.getDownloadStatus()).isEqualTo(AttachmentDownloadStatus.SUCCESS);
            assertThat(a.getSourceUrl()).startsWith("piee:" + SOL_NO + "/");
        });
        assertThat(rows).extracting(OpportunityAttachment::getContentSha256)
                .containsExactly(sha("rfp"), sha("sow"), sha("wage"));
        assertThat(piee.lastPostForm).doesNotContain("visibleField");
        verifyFinished(PieeFetchStatus.DONE, 3);
    }

    // TC-PIEE-002
    @Test
    @DisplayName("PIEE첫화면으로넘어감_PIEE에없음")
    void PIEE첫화면으로넘어감_PIEE에없음() {
        piee.noticeExists = false;

        service.fetch(opp.getId());

        assertThat(piee.postCount.get()).isZero();
        verify(attachmentRepository, never()).save(any(OpportunityAttachment.class));
        verifyFinished(PieeFetchStatus.NOT_FOUND, null);
    }

    // TC-PIEE-003
    @Test
    @DisplayName("받기단추없음_0개받음")
    void 받기단추없음_0개받음() {
        piee.downloadButton = false;

        service.fetch(opp.getId());

        assertThat(piee.postCount.get()).isZero();
        verifyFinished(PieeFetchStatus.DONE, 0);
    }

    // TC-PIEE-004
    @Test
    @DisplayName("압축무게상한넘음_받기실패")
    void 압축무게상한넘음_받기실패() throws Exception {
        piee.postBody = FakePieeServer.zip(FakePieeServer.files("RFP.pdf", "x".repeat(5000)));
        given(settings.maxZipBytes()).willReturn(100L);

        service.fetch(opp.getId());

        verify(attachmentRepository, never()).save(any(OpportunityAttachment.class));
        verify(opportunityRepository).finishPieeFetch(eq(opp.getId()), eq(PieeFetchStatus.FETCHING),
                eq(PieeFetchStatus.FAILED), eq(NOW), isNull(), startsWith("압축 무게 상한"));
    }

    // TC-PIEE-005
    @Test
    @DisplayName("압축아닌응답_받기실패")
    void 압축아닌응답_받기실패() {
        piee.postBody = "<html>Session expired</html>".getBytes(StandardCharsets.UTF_8);

        service.fetch(opp.getId());

        verify(attachmentRepository, never()).save(any(OpportunityAttachment.class));
        verify(opportunityRepository).finishPieeFetch(eq(opp.getId()), eq(PieeFetchStatus.FETCHING),
                eq(PieeFetchStatus.FAILED), eq(NOW), isNull(), eq("받은 응답이 압축이 아님"));
    }

    // TC-PIEE-006
    @Test
    @DisplayName("같은공고_내용같은파일_넣지않음")
    void 같은공고_내용같은파일_넣지않음() throws Exception {
        OpportunityAttachment sam = OpportunityAttachment.builder().opportunity(opp).fileName("rfp_sam.pdf")
                .sourceUrl("https://sam.gov/x/resources/files/1/download").storageUrl("stored/rfp_sam.pdf")
                .downloadStatus(AttachmentDownloadStatus.SUCCESS).contentSha256(sha("rfp")).build();
        existing.add(sam);
        piee.postBody = FakePieeServer.zip(FakePieeServer.files("RFP.pdf", "rfp"));

        service.fetch(opp.getId());

        verify(attachmentRepository, never()).save(any(OpportunityAttachment.class));
        verify(storageService, never()).store(anyString(), anyString(), any(InputStream.class));
        verifyFinished(PieeFetchStatus.DONE, 1);
    }

    // TC-PIEE-007
    @Test
    @DisplayName("다른판에같은파일저장됨_저장같이씀")
    void 다른판에같은파일저장됨_저장같이씀() throws Exception {
        OpportunityAttachment previous = OpportunityAttachment.builder().fileName("RFP.pdf")
                .sourceUrl("piee:" + SOL_NO + "/RFP.pdf").storageUrl("opportunity-attachments/prev/RFP.pdf")
                .downloadStatus(AttachmentDownloadStatus.SUCCESS).contentSha256(sha("rfp")).build();
        given(attachmentRepository.findSharable(sha("rfp"), SOL_NO, opp.getId(),
                AttachmentDownloadStatus.SUCCESS, AttachmentFiles.ADMIN_UPLOAD_SOURCE)).willReturn(List.of(previous));
        piee.postBody = FakePieeServer.zip(FakePieeServer.files("RFP.pdf", "rfp"));

        service.fetch(opp.getId());

        assertThat(savedRows()).singleElement()
                .extracting(OpportunityAttachment::getStorageUrl).isEqualTo("opportunity-attachments/prev/RFP.pdf");
        verify(storageService, never()).store(anyString(), anyString(), any(InputStream.class));
    }

    // TC-PIEE-009
    @Test
    @DisplayName("가져와야함첨부_같은이름_그행채움")
    void 가져와야함첨부_같은이름_그행채움() throws Exception {
        OpportunityAttachment manual = OpportunityAttachment.builder().opportunity(opp).fileName("SOW.docx")
                .sourceUrl("https://piee.eb.mil/some/link").downloadStatus(AttachmentDownloadStatus.MANUAL_FETCH_REQUIRED)
                .build();
        existing.add(manual);
        piee.postBody = FakePieeServer.zip(FakePieeServer.files("SOW.docx", "sow"));

        service.fetch(opp.getId());

        assertThat(savedRows()).singleElement().isSameAs(manual);
        assertThat(manual.getDownloadStatus()).isEqualTo(AttachmentDownloadStatus.SUCCESS);
    }

    // TC-PIEE-010
    @Test
    @DisplayName("해시빈옛첨부_받을때채움")
    void 해시빈옛첨부_받을때채움() throws Exception {
        OpportunityAttachment old = OpportunityAttachment.builder().opportunity(opp).fileName("RFP.pdf")
                .sourceUrl("https://sam.gov/x/resources/files/1/download").storageUrl("stored/old-rfp.pdf")
                .downloadStatus(AttachmentDownloadStatus.SUCCESS).build();
        existing.add(old);
        given(storageService.load("stored/old-rfp.pdf")).willReturn("rfp".getBytes(StandardCharsets.UTF_8));
        piee.postBody = FakePieeServer.zip(FakePieeServer.files("RFP.pdf", "rfp"));

        service.fetch(opp.getId());

        assertThat(old.getContentSha256()).isEqualTo(sha("rfp"));
        assertThat(savedRows()).singleElement().isSameAs(old);
    }
}
