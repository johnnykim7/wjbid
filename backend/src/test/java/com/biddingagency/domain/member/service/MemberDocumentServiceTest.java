package com.biddingagency.domain.member.service;

import com.biddingagency.TestHelper;
import com.biddingagency.controller.MemberDocumentController;
import com.biddingagency.controller.MemberDocumentController.FixedSlot;
import com.biddingagency.domain.member.entity.DocumentCategory;
import com.biddingagency.domain.member.entity.DocumentLanguage;
import com.biddingagency.domain.member.entity.Member;
import com.biddingagency.domain.member.entity.MemberDocument;
import com.biddingagency.domain.member.repository.MemberDocumentRepository;
import com.biddingagency.integration.storage.StorageService;
import com.biddingagency.security.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

/**
 * 서류함 고정 칸과 국문·영문 칸 (CR-147, BIZ-065, BIZ-066, T3-5 TC-DOC-201~209)
 */
@ExtendWith(MockitoExtension.class)
class MemberDocumentServiceTest {

    @Mock private MemberDocumentRepository repository;
    @Mock private StorageService storageService;

    @InjectMocks
    private MemberDocumentService service;

    private final UUID owner = UUID.randomUUID();

    private static MockMultipartFile file(String name) {
        return new MockMultipartFile("file", name, "application/pdf", new byte[]{1, 2, 3});
    }

    private MemberDocument stored(DocumentCategory category, DocumentLanguage language) {
        return TestHelper.withId(MemberDocument.builder().memberId(owner).category(category).language(language)
                .fileName(category.name() + "-" + language + ".pdf").storageUrl("old").build());
    }

    private void newUpload() {
        given(storageService.store(anyString(), any())).willReturn("url");
        given(repository.save(any(MemberDocument.class))).willAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("TC-DOC-201 국문·영문 칸에 국문으로 올리면 국문 파일로 저장하고 제목에 국문을 붙인다")
    void 국문영문칸국문_올리기_국문으로저장() {
        newUpload();
        given(repository.findFirstByMemberIdAndCategoryAndLanguage(owner,
                DocumentCategory.BUSINESS_REGISTRATION_CERTIFICATE, DocumentLanguage.KO)).willReturn(Optional.empty());

        MemberDocument d = service.upload(owner, DocumentCategory.BUSINESS_REGISTRATION_CERTIFICATE, DocumentLanguage.KO,
                null, null, file("ko.pdf"), MemberDocumentService.ExpiryChange.KEEP);

        assertThat(tuple(d.getLanguage(), d.getDisplayName())).isEqualTo(tuple(DocumentLanguage.KO, "사업자등록증명원(국문)"));
    }

    @Test
    @DisplayName("TC-DOC-202 같은 언어로 다시 올리면 그 언어 서류의 파일만 바꾼다")
    void 같은언어다시올림_올리기_그언어파일만교체() {
        MemberDocument ko = stored(DocumentCategory.COMPANY_PROFILE, DocumentLanguage.KO);
        given(repository.findFirstByMemberIdAndCategoryAndLanguage(owner, DocumentCategory.COMPANY_PROFILE,
                DocumentLanguage.KO)).willReturn(Optional.of(ko));
        given(repository.findById(ko.getId())).willReturn(Optional.of(ko));
        given(storageService.store(anyString(), any())).willReturn("url");

        MemberDocument d = service.upload(owner, DocumentCategory.COMPANY_PROFILE, DocumentLanguage.KO,
                null, null, file("new.pdf"), MemberDocumentService.ExpiryChange.KEEP);

        assertThat(tuple(d, d.getFileName(), d.getLanguage())).isEqualTo(tuple(ko, "new.pdf", DocumentLanguage.KO));
    }

    @Test
    @DisplayName("TC-DOC-203 국문·영문 칸에 언어 없이 올리면 400")
    void 국문영문칸언어없음_올리기_400() {
        ResponseStatusException e = catchThrowableOfType(() -> service.upload(owner, DocumentCategory.COMPANY_PROFILE,
                null, null, null, file("a.pdf"), MemberDocumentService.ExpiryChange.KEEP), ResponseStatusException.class);

        assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("TC-DOC-204 국문·영문 칸이 아니면 보낸 언어를 두지 않는다")
    void 다른칸언어보냄_올리기_언어무시() {
        newUpload();
        given(repository.findFirstByMemberIdAndCategory(owner, DocumentCategory.BUSINESS_REGISTRATION))
                .willReturn(Optional.empty());

        MemberDocument d = service.upload(owner, DocumentCategory.BUSINESS_REGISTRATION, DocumentLanguage.EN,
                null, null, file("biz.pdf"), MemberDocumentService.ExpiryChange.KEEP);

        assertThat(tuple(d.getLanguage(), d.getDisplayName())).isEqualTo(tuple(null, "사업자등록증"));
    }

    @Test
    @DisplayName("TC-DOC-205 국문·영문 칸에서 한 건을 고르면 영문이 먼저다")
    void 영문있음_서류함에서고르기_영문() {
        MemberDocument en = stored(DocumentCategory.BUSINESS_REGISTRATION_CERTIFICATE, DocumentLanguage.EN);
        given(repository.findFirstByMemberIdAndCategoryAndLanguage(owner,
                DocumentCategory.BUSINESS_REGISTRATION_CERTIFICATE, DocumentLanguage.EN)).willReturn(Optional.of(en));

        assertThat(service.findFixed(owner, DocumentCategory.BUSINESS_REGISTRATION_CERTIFICATE)).isEqualTo(en);
    }

    @Test
    @DisplayName("TC-DOC-206 국문·영문 칸에 영문이 없으면 국문을 고른다")
    void 영문없음_서류함에서고르기_국문() {
        MemberDocument ko = stored(DocumentCategory.COMPANY_PROFILE, DocumentLanguage.KO);
        given(repository.findFirstByMemberIdAndCategoryAndLanguage(owner, DocumentCategory.COMPANY_PROFILE,
                DocumentLanguage.EN)).willReturn(Optional.empty());
        given(repository.findFirstByMemberIdAndCategoryAndLanguage(owner, DocumentCategory.COMPANY_PROFILE,
                DocumentLanguage.KO)).willReturn(Optional.of(ko));

        assertThat(service.findFixed(owner, DocumentCategory.COMPANY_PROFILE)).isEqualTo(ko);
    }

    @Test
    @DisplayName("TC-DOC-207 사업자등록증명원 글은 사업자등록증이 아니라 사업자등록증명원 칸으로 맞춘다")
    void 사업자등록증명원글_분류찾기_증명원() {
        assertThat(DocumentCategory.matchByText("사업자등록증명원 사본"))
                .isEqualTo(DocumentCategory.BUSINESS_REGISTRATION_CERTIFICATE);
    }

    @Test
    @DisplayName("TC-DOC-208 사업자등록증 글은 사업자등록증 칸으로 맞춘다")
    void 사업자등록증글_분류찾기_사업자등록증() {
        assertThat(DocumentCategory.matchByText("사업자등록증")).isEqualTo(DocumentCategory.BUSINESS_REGISTRATION);
    }

    @Test
    @DisplayName("TC-DOC-209 서류함 조회는 고정 칸 다섯을 내고 국문·영문 칸은 국문, 영문 두 줄이다")
    void 서류함조회_고정칸_국문영문두줄() {
        MemberDocument ko = stored(DocumentCategory.COMPANY_PROFILE, DocumentLanguage.KO);
        MemberDocumentService docs = org.mockito.Mockito.mock(MemberDocumentService.class);
        given(docs.list(owner)).willReturn(List.of(ko));
        Member member = TestHelper.withId(Member.builder().email("t@t.com").passwordHash("h").companyName("Co").build(), owner);
        MemberDocumentController controller = new MemberDocumentController(docs, null);

        List<FixedSlot> slots = controller.vault(new CustomUserDetails(member, List.of())).getBody().fixedSlots();

        assertThat(slots).extracting(FixedSlot::category, s -> s.files().stream()
                        .map(f -> tuple(f.language(), f.document() == null ? null : f.document().id())).toList())
                .containsExactly(
                        tuple("BUSINESS_REGISTRATION", List.of(tuple(null, null))),
                        tuple("BUSINESS_REGISTRATION_CERTIFICATE", List.of(tuple("KO", null), tuple("EN", null))),
                        tuple("COMPANY_PROFILE", List.of(tuple("KO", ko.getId().toString()), tuple("EN", null))),
                        tuple("FINANCIAL_STATEMENT", List.of(tuple(null, null))),
                        tuple("LICENSE", List.of(tuple(null, null))));
    }
}
