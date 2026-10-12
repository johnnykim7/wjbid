package com.biddingagency.domain.compliance.service;

import com.biddingagency.domain.bid.entity.BidRequest;
import com.biddingagency.domain.bid.entity.BidRequestState;
import com.biddingagency.domain.bid.entity.ClientDocument;
import com.biddingagency.domain.bid.repository.BidRequestRepository;
import com.biddingagency.domain.bid.repository.ClientDocumentRepository;
import com.biddingagency.domain.compliance.entity.FulfillmentParty;
import com.biddingagency.domain.compliance.entity.FulfillmentStatus;
import com.biddingagency.domain.compliance.entity.RequirementFulfillmentMap;
import com.biddingagency.domain.compliance.repository.RequirementFulfillmentMapRepository;
import com.biddingagency.domain.member.entity.Member;
import com.biddingagency.domain.member.entity.MemberDocument;
import com.biddingagency.domain.member.service.MemberDocumentService;
import com.biddingagency.domain.notice.repository.NoticeRepository;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.entity.OpportunityRequirementItem;
import com.biddingagency.domain.opportunity.entity.RequirementCategory;
import com.biddingagency.domain.opportunity.repository.OpportunityRequirementItemRepository;
import com.biddingagency.domain.setting.service.SystemSettingService;
import com.biddingagency.dto.PreCheckResponses.Detail;
import com.biddingagency.dto.PreCheckResponses.Summary;
import com.biddingagency.integration.storage.StorageService;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * TC-PRE-018 ~ TC-PRE-025: 제출 전 검사 조회와 요구 서류 잇기 (CR-141, BIZ-046, BIZ-049)
 */
@ExtendWith(MockitoExtension.class)
class PreCheckServiceTest {

    @Mock private BidRequestRepository bidRequestRepository;
    @Mock private OpportunityRequirementItemRepository requirementRepository;
    @Mock private RequirementFulfillmentMapRepository fulfillmentMapRepository;
    @Mock private ClientDocumentRepository clientDocumentRepository;
    @Mock private NoticeRepository noticeRepository;
    @Mock private MemberDocumentService memberDocumentService;
    @Mock private StorageService storageService;
    @Mock private SystemSettingService systemSettingService;

    private PreCheckService service;

    private final UUID opportunityId = UUID.randomUUID();
    private final UUID bidRequestId = UUID.randomUUID();
    private Member member;
    private Opportunity opportunity;
    private BidRequest bidRequest;

    @BeforeEach
    void setUp() {
        service = new PreCheckService(bidRequestRepository, requirementRepository, fulfillmentMapRepository,
                clientDocumentRepository, noticeRepository, memberDocumentService, storageService,
                new PreCheckJudge(systemSettingService), new PdfPageCounter(), DataSize.ofMegabytes(50));
        member = member();
        opportunity = Opportunity.builder().build();
        ReflectionTestUtils.setField(opportunity, "id", opportunityId);
        bidRequest = BidRequest.builder().member(member).opportunity(opportunity).state(BidRequestState.DOCS_PENDING).build();
        ReflectionTestUtils.setField(bidRequest, "id", bidRequestId);
    }

    private static Member member() {
        Member m = Member.builder().email("customer@example.com").build();
        ReflectionTestUtils.setField(m, "id", UUID.randomUUID());
        return m;
    }

    private OpportunityRequirementItem item(String title, FulfillmentParty party, boolean mandatory, String format) {
        Map<String, Object> json = new HashMap<>();
        json.put(RequirementSyncService.KEY_FULFILLMENT_PARTY, party.name());
        json.put(RequirementSyncService.KEY_MANDATORY, mandatory);
        if (format != null) {
            json.put(RequirementSyncService.KEY_FORMAT, format);
        }
        OpportunityRequirementItem item = OpportunityRequirementItem.builder()
                .opportunity(opportunity)
                .category(RequirementCategory.DOCUMENT)
                .title(title)
                .isBlocker(mandatory && party == FulfillmentParty.CLIENT_UPLOAD)
                .requirementJson(json)
                .build();
        ReflectionTestUtils.setField(item, "id", UUID.randomUUID());
        return item;
    }

    private RequirementFulfillmentMap linked(OpportunityRequirementItem item, String fileName) {
        ClientDocument doc = ClientDocument.builder().fileName(fileName).fileSize(10L).build();
        ReflectionTestUtils.setField(doc, "id", UUID.randomUUID());
        return RequirementFulfillmentMap.builder()
                .bidRequest(bidRequest)
                .requirementItem(item)
                .clientDocument(doc)
                .status(FulfillmentStatus.FULFILLED)
                .build();
    }

    private void givenDetail(List<OpportunityRequirementItem> items, List<RequirementFulfillmentMap> maps) {
        given(bidRequestRepository.findByIdWithDetails(bidRequestId)).willReturn(Optional.of(bidRequest));
        given(requirementRepository.findByOpportunityIdAndReqIdIsNullOrderBySortOrderAsc(opportunityId)).willReturn(items);
        given(fulfillmentMapRepository.findWithClientDocumentByBidRequestIds(List.of(bidRequestId))).willReturn(maps);
    }

    private void givenSaveReturnsArgument() {
        given(clientDocumentRepository.save(any(ClientDocument.class))).willAnswer(inv -> inv.getArgument(0));
        given(fulfillmentMapRepository.save(any(RequirementFulfillmentMap.class))).willAnswer(inv -> inv.getArgument(0));
    }

    private ClientDocument savedClientDocument() {
        ArgumentCaptor<ClientDocument> captor = ArgumentCaptor.forClass(ClientDocument.class);
        then(clientDocumentRepository).should().save(captor.capture());
        return captor.getValue();
    }

    private static byte[] pdf(int pages) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PdfDocument pdf = new PdfDocument(new PdfWriter(out))) {
            for (int i = 0; i < pages; i++) {
                pdf.addNewPage();
            }
        }
        return out.toByteArray();
    }

    private static HttpStatus statusOf(Throwable e) {
        return HttpStatus.valueOf(((ResponseStatusException) e).getStatusCode().value());
    }

    @Test
    @DisplayName("TC-PRE-018 필수빠짐_검사_전환불가")
    void 필수빠짐_검사_전환불가() {
        OpportunityRequirementItem license = item("Business License", FulfillmentParty.CLIENT_UPLOAD, true, null);
        OpportunityRequirementItem insurance = item("Insurance Certificate", FulfillmentParty.CLIENT_UPLOAD, true, null);
        givenDetail(List.of(license, insurance), List.of(linked(license, "license.pdf")));

        Detail detail = service.detail(member.getId(), bidRequestId);

        assertThat(detail.summary()).isEqualTo(new Summary(2, 1, 1, 0, 0));
        assertThat(detail.canTransition()).isFalse();
    }

    @Test
    @DisplayName("TC-PRE-019 필수다있고어긋남_검사_전환가능")
    void 필수다있고어긋남_검사_전환가능() {
        OpportunityRequirementItem license = item("Business License", FulfillmentParty.CLIENT_UPLOAD, true, "PDF");
        OpportunityRequirementItem insurance = item("Insurance Certificate", FulfillmentParty.CLIENT_UPLOAD, true, null);
        givenDetail(List.of(license, insurance),
                List.of(linked(license, "license.docx"), linked(insurance, "insurance.pdf")));

        Detail detail = service.detail(member.getId(), bidRequestId);

        assertThat(detail.summary().mismatch()).isEqualTo(1);
        assertThat(detail.canTransition()).isTrue();
    }

    @Test
    @DisplayName("TC-PRE-020 남의입찰건_검사_찾을수없음")
    void 남의입찰건_검사_찾을수없음() {
        given(bidRequestRepository.findByIdWithDetails(bidRequestId)).willReturn(Optional.of(bidRequest));

        assertThatThrownBy(() -> service.detail(UUID.randomUUID(), bidRequestId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("TC-PRE-021 분석전_검사_숫자없음")
    void 분석전_검사_숫자없음() {
        givenDetail(List.of(), List.of());

        Detail detail = service.detail(member.getId(), bidRequestId);

        assertThat(detail.analysisReady()).isFalse();
        assertThat(detail.summary()).isNull();
        assertThat(detail.canTransition()).isTrue();
    }

    @Test
    @DisplayName("TC-PRE-022 서류함서류_고르기_복사해이음")
    void 서류함서류_고르기_복사해이음() throws Exception {
        OpportunityRequirementItem license = item("Business License", FulfillmentParty.CLIENT_UPLOAD, true, "PDF");
        UUID vaultDocId = UUID.randomUUID();
        MemberDocument vaultDoc = MemberDocument.builder()
                .memberId(member.getId()).fileName("license.pdf").fileSize(100L)
                .contentType("application/pdf").storageUrl("member-docs/license.pdf").build();
        given(bidRequestRepository.findByIdWithDetails(bidRequestId)).willReturn(Optional.of(bidRequest));
        given(requirementRepository.findById(license.getId())).willReturn(Optional.of(license));
        given(memberDocumentService.getOwned(member.getId(), vaultDocId)).willReturn(vaultDoc);
        given(storageService.load("member-docs/license.pdf")).willReturn(pdf(2));
        given(storageService.store(anyString(), anyString(), any(byte[].class))).willReturn("client-docs/license.pdf");
        givenSaveReturnsArgument();

        service.fromVault(member, bidRequestId, license.getId(), vaultDocId);

        ClientDocument saved = savedClientDocument();
        assertThat(saved.getDocumentCategory()).isEqualTo("Business License");
        assertThat(saved.getPageCount()).isEqualTo(2);
        ArgumentCaptor<RequirementFulfillmentMap> map = ArgumentCaptor.forClass(RequirementFulfillmentMap.class);
        then(fulfillmentMapRepository).should().save(map.capture());
        assertThat(map.getValue().getStatus()).isEqualTo(FulfillmentStatus.FULFILLED);
    }

    @Test
    @DisplayName("TC-PRE-023 남의서류함서류_고르기_찾을수없음")
    void 남의서류함서류_고르기_찾을수없음() {
        OpportunityRequirementItem license = item("Business License", FulfillmentParty.CLIENT_UPLOAD, true, null);
        UUID otherDocId = UUID.randomUUID();
        given(bidRequestRepository.findByIdWithDetails(bidRequestId)).willReturn(Optional.of(bidRequest));
        given(requirementRepository.findById(license.getId())).willReturn(Optional.of(license));
        given(memberDocumentService.getOwned(member.getId(), otherDocId))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "서류를 찾을 수 없습니다"));

        assertThatThrownBy(() -> service.fromVault(member, bidRequestId, license.getId(), otherDocId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.NOT_FOUND));
        then(fulfillmentMapRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("TC-PRE-024 플랫폼작성항목_올리기_거부")
    void 플랫폼작성항목_올리기_거부() throws Exception {
        OpportunityRequirementItem pastPerformance = item("Past Performance", FulfillmentParty.PLATFORM_GENERATED, true, null);
        given(bidRequestRepository.findByIdWithDetails(bidRequestId)).willReturn(Optional.of(bidRequest));
        given(requirementRepository.findById(pastPerformance.getId())).willReturn(Optional.of(pastPerformance));
        MockMultipartFile file = new MockMultipartFile("file", "a.pdf", "application/pdf", pdf(1));

        assertThatThrownBy(() -> service.upload(member, bidRequestId, pastPerformance.getId(), file))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("TC-PRE-027 서류접수뒤_올리기_거부")
    void 서류접수뒤_올리기_거부() throws Exception {
        bidRequest = BidRequest.builder().member(member).opportunity(opportunity).state(BidRequestState.DOCS_RECEIVED).build();
        ReflectionTestUtils.setField(bidRequest, "id", bidRequestId);
        given(bidRequestRepository.findByIdWithDetails(bidRequestId)).willReturn(Optional.of(bidRequest));
        MockMultipartFile file = new MockMultipartFile("file", "a.pdf", "application/pdf", pdf(1));

        assertThatThrownBy(() -> service.upload(member, bidRequestId, UUID.randomUUID(), file))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("TC-PRE-025 PDF올림_올리기_쪽수저장")
    void PDF올림_올리기_쪽수저장() throws Exception {
        OpportunityRequirementItem license = item("Business License", FulfillmentParty.CLIENT_UPLOAD, true, null);
        given(bidRequestRepository.findByIdWithDetails(bidRequestId)).willReturn(Optional.of(bidRequest));
        given(requirementRepository.findById(license.getId())).willReturn(Optional.of(license));
        given(storageService.store(anyString(), anyString(), any(byte[].class))).willReturn("client-docs/a.pdf");
        givenSaveReturnsArgument();
        MockMultipartFile file = new MockMultipartFile("file", "a.pdf", "application/pdf", pdf(3));

        service.upload(member, bidRequestId, license.getId(), file);

        assertThat(savedClientDocument().getPageCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("TC-COMP-027 고객서류올림_매트릭스줄_찾을수없음")
    void 고객서류올림_매트릭스줄_찾을수없음() throws Exception {
        OpportunityRequirementItem matrixLine = item("The offeror shall submit a quality control plan.",
                FulfillmentParty.CLIENT_UPLOAD, true, null);
        ReflectionTestUtils.setField(matrixLine, "reqId", "REQ-001");
        given(bidRequestRepository.findByIdWithDetails(bidRequestId)).willReturn(Optional.of(bidRequest));
        given(requirementRepository.findById(matrixLine.getId())).willReturn(Optional.of(matrixLine));
        MockMultipartFile file = new MockMultipartFile("file", "a.pdf", "application/pdf", pdf(1));

        assertThatThrownBy(() -> service.upload(member, bidRequestId, matrixLine.getId(), file))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.NOT_FOUND));
        then(fulfillmentMapRepository).should(never()).save(any());
    }
}
