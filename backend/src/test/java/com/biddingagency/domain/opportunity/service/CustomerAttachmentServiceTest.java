package com.biddingagency.domain.opportunity.service;

import com.biddingagency.TestHelper;
import com.biddingagency.domain.member.MembershipError;
import com.biddingagency.domain.member.MembershipException;
import com.biddingagency.domain.member.repository.MemberMembershipRepository;
import com.biddingagency.domain.member.repository.MemberRepository;
import com.biddingagency.domain.member.service.MembershipService;
import com.biddingagency.domain.opportunity.dto.CustomerAttachmentDto;
import com.biddingagency.domain.opportunity.entity.AttachmentDownloadStatus;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.entity.OpportunityAttachment;
import com.biddingagency.domain.opportunity.repository.OpportunityAttachmentRepository;
import com.biddingagency.domain.opportunity.repository.OpportunityRepository;
import com.biddingagency.integration.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * TC-BROWSE-018~021 (CR-168): 고객 첨부 칸은 받아 둔 첨부만 보이고, 로그인한 고객은 회원 구분 없이 내려받는다(BIZ-084).
 */
@ExtendWith(MockitoExtension.class)
class CustomerAttachmentServiceTest {

    private static final String SAM_URL = "https://sam.gov/api/prod/opps/v3/opportunities/resources/files/abc/download";

    @Mock private OpportunityRepository opportunityRepository;
    @Mock private OpportunityAttachmentRepository attachmentRepository;
    @Mock private StorageService storageService;
    @Mock private MemberMembershipRepository membershipRepository;
    @Mock private MemberRepository memberRepository;

    private CustomerAttachmentService service;
    private Opportunity opp;

    @BeforeEach
    void setUp() {
        MembershipService membershipService = new MembershipService(membershipRepository, memberRepository, Clock.systemUTC());
        service = new CustomerAttachmentService(opportunityRepository, attachmentRepository, storageService, membershipService);
        opp = opportunity();
    }

    @Test
    @DisplayName("TC-BROWSE-018 받아둔첨부만_목록_출처구분")
    void 받아둔첨부만_목록_출처구분() {
        given(opportunityRepository.existsById(opp.getId())).willReturn(true);
        given(attachmentRepository.findByOpportunityId(opp.getId())).willReturn(List.of(
                stored(opp, "a-sam.pdf", SAM_URL),
                stored(opp, "b-piee.pdf", PieeFetchService.SOURCE_PREFIX + "W90VN626QA018/b-piee.pdf"),
                stored(opp, "c-upload.pdf", AttachmentFiles.ADMIN_UPLOAD_SOURCE),
                TestHelper.withId(OpportunityAttachment.builder().opportunity(opp).fileName("d-failed.pdf")
                        .sourceUrl(SAM_URL).downloadStatus(AttachmentDownloadStatus.FAILED).build())));

        List<CustomerAttachmentDto> rows = service.list(opp.getId());

        assertThat(rows).extracting(CustomerAttachmentDto::fileName, CustomerAttachmentDto::source, CustomerAttachmentDto::sourceUrl)
                .containsExactly(
                        tuple("a-sam.pdf", CustomerAttachmentDto.Source.SAM, SAM_URL),
                        tuple("b-piee.pdf", CustomerAttachmentDto.Source.PIEE, null),
                        tuple("c-upload.pdf", CustomerAttachmentDto.Source.UPLOAD, null));
    }

    @Test
    @DisplayName("TC-BROWSE-019 비로그인_내려받기_로그인필요")
    void 비로그인_내려받기_로그인필요() {
        MembershipException e = catchThrowableOfType(
                () -> service.load(null, opp.getId(), UUID.randomUUID()), MembershipException.class);

        assertThat(e.getError()).isEqualTo(MembershipError.LOGIN_REQUIRED);
        verify(storageService, never()).load(anyString());
    }

    @Test
    @DisplayName("TC-BROWSE-020 무료회원_내려받기_파일내용")
    void 무료회원_내려받기_파일내용() {
        OpportunityAttachment piee = stored(opp, "b-piee.pdf", PieeFetchService.SOURCE_PREFIX + "W90VN626QA018/b-piee.pdf");
        given(attachmentRepository.findById(piee.getId())).willReturn(Optional.of(piee));
        given(storageService.load(piee.getStorageUrl())).willReturn(new byte[]{1, 2, 3});

        Optional<CustomerAttachmentService.Download> file = service.load(UUID.randomUUID(), opp.getId(), piee.getId());

        assertThat(file).get().extracting(CustomerAttachmentService.Download::fileName,
                        CustomerAttachmentService.Download::contentType, d -> d.content().length)
                .containsExactly("b-piee.pdf", "application/pdf", 3);
    }

    @Test
    @DisplayName("TC-BROWSE-021 다른공고첨부_또는_저장안된첨부_없음")
    void 다른공고첨부_또는_저장안된첨부_없음() {
        OpportunityAttachment other = stored(opportunity(), "x.pdf", SAM_URL);
        OpportunityAttachment failed = TestHelper.withId(OpportunityAttachment.builder().opportunity(opp).fileName("d.pdf")
                .sourceUrl(SAM_URL).downloadStatus(AttachmentDownloadStatus.FAILED).build());
        given(attachmentRepository.findById(other.getId())).willReturn(Optional.of(other));
        given(attachmentRepository.findById(failed.getId())).willReturn(Optional.of(failed));
        UUID memberId = UUID.randomUUID();

        assertThat(List.of(service.load(memberId, opp.getId(), other.getId()),
                service.load(memberId, opp.getId(), failed.getId()))).allMatch(Optional::isEmpty);
    }

    private static Opportunity opportunity() {
        Opportunity o = Opportunity.builder().noticeId("N-" + UUID.randomUUID()).title("t").active(true)
                .firstSeenAt(LocalDateTime.now()).lastModifiedAt(LocalDateTime.now()).build();
        ReflectionTestUtils.setField(o, "id", UUID.randomUUID());
        return o;
    }

    private static OpportunityAttachment stored(Opportunity o, String fileName, String sourceUrl) {
        OpportunityAttachment a = TestHelper.withId(OpportunityAttachment.builder().opportunity(o)
                .sourceUrl(sourceUrl).build());
        a.applyUpload(fileName, 3L, "application/pdf", "opportunity-attachments/" + o.getId() + "/" + fileName);
        return a;
    }
}
