package com.biddingagency.domain.member.service;

import com.biddingagency.domain.member.entity.DocumentCategory;
import com.biddingagency.domain.member.entity.MemberDocument;
import com.biddingagency.domain.member.repository.MemberDocumentRepository;
import com.biddingagency.integration.storage.StorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.BDDMockito.given;

/**
 * TC-MKT-041 ~ TC-MKT-042 (CR-136): 서류 만료일 고치기(BID-MKT-012).
 */
@ExtendWith(MockitoExtension.class)
class MemberDocumentServiceExpiryTest {

    private static final LocalDate EXPIRES = LocalDate.of(2026, 12, 31);

    @Mock private MemberDocumentRepository repository;
    @Mock private StorageService storageService;

    @InjectMocks
    private MemberDocumentService service;

    private final UUID owner = UUID.randomUUID();

    private MemberDocument stored() {
        MemberDocument d = MemberDocument.builder().memberId(owner).category(DocumentCategory.BUSINESS_REGISTRATION)
                .fileName("biz.pdf").expiresOn(EXPIRES).build();
        UUID id = UUID.randomUUID();
        ReflectionTestUtils.setField(d, "id", id);
        given(repository.findById(id)).willReturn(Optional.of(d));
        return d;
    }

    @Test
    @DisplayName("TC-MKT-041 남의 서류 만료일은 고치지 못하고 값도 그대로다")
    void 남의서류_만료일고치기_거부() {
        MemberDocument d = stored();

        ResponseStatusException e = catchThrowableOfType(
                () -> service.changeExpiry(UUID.randomUUID(), d.getId(), LocalDate.of(2027, 1, 1)),
                ResponseStatusException.class);

        assertThat(tuple(e.getStatusCode(), d.getExpiresOn())).isEqualTo(tuple(HttpStatus.NOT_FOUND, EXPIRES));
    }

    @Test
    @DisplayName("TC-MKT-042 만료일을 비우면 만료일 없음으로 저장한다")
    void 만료일지움_만료일고치기_빈값저장() {
        MemberDocument d = stored();

        service.changeExpiry(owner, d.getId(), null);

        assertThat(d.getExpiresOn()).isNull();
    }
}
