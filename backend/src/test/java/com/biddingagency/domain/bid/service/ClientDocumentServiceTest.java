package com.biddingagency.domain.bid.service;

import com.biddingagency.domain.bid.BidRequestNotFoundException;
import com.biddingagency.domain.bid.entity.BidRequest;
import com.biddingagency.domain.bid.entity.ClientDocument;
import com.biddingagency.domain.bid.repository.ClientDocumentRepository;
import com.biddingagency.domain.member.entity.Member;
import com.biddingagency.integration.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * TC-OWN-007 ~ TC-OWN-011: 올린 서류는 그 입찰 건을 낸 회원과 관리자만 (BIZ-060)
 */
@ExtendWith(MockitoExtension.class)
class ClientDocumentServiceTest {

    @Mock
    private ClientDocumentRepository clientDocumentRepository;
    @Mock
    private BidRequestService bidRequestService;
    @Mock
    private StorageService storageService;

    private ClientDocumentService clientDocumentService;

    @BeforeEach
    void setUp() {
        clientDocumentService = new ClientDocumentService(
                clientDocumentRepository, bidRequestService, storageService, DataSize.ofMegabytes(50));
    }

    @Test
    @DisplayName("TC-OWN-007 남의건_서류목록_404_서류조회없음")
    void 남의건_list_막힘() {
        // given
        UUID bidRequestId = UUID.randomUUID();
        Member other = member(Member.Role.CUSTOMER);
        given(bidRequestService.findAccessible(bidRequestId, other)).willThrow(new BidRequestNotFoundException());

        // when & then
        assertThatThrownBy(() -> clientDocumentService.list(bidRequestId, other))
                .isInstanceOf(BidRequestNotFoundException.class);
        then(clientDocumentRepository).should(never()).findByBidRequestId(any());
    }

    @Test
    @DisplayName("TC-OWN-008 남의건_서류올리기_404_파일저장없음")
    void 남의건_upload_막힘() {
        // given
        UUID bidRequestId = UUID.randomUUID();
        Member other = member(Member.Role.CUSTOMER);
        MockMultipartFile file = new MockMultipartFile("file", "a.pdf", "application/pdf", new byte[]{1});
        given(bidRequestService.findAccessible(bidRequestId, other)).willThrow(new BidRequestNotFoundException());

        // when & then
        assertThatThrownBy(() -> clientDocumentService.upload(bidRequestId, other, file, "cat"))
                .isInstanceOf(BidRequestNotFoundException.class);
        then(storageService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("TC-OWN-009 다른건서류번호_지우기_404_지우지않음")
    void 다른건서류_delete_막힘() {
        // given
        Member owner = member(Member.Role.CUSTOMER);
        BidRequest mine = bidRequest(owner);
        BidRequest others = bidRequest(member(Member.Role.CUSTOMER));
        ClientDocument othersDoc = ClientDocument.builder().bidRequest(others).build();
        UUID docId = UUID.randomUUID();
        given(bidRequestService.findAccessible(mine.getId(), owner)).willReturn(mine);
        given(clientDocumentRepository.findById(docId)).willReturn(Optional.of(othersDoc));

        // when & then
        assertThatThrownBy(() -> clientDocumentService.delete(mine.getId(), docId, owner))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        then(clientDocumentRepository).should(never()).delete(any());
    }

    @Test
    @DisplayName("TC-OWN-010 본인건서류_지우기_지움")
    void 본인건서류_delete_지움() {
        // given
        Member owner = member(Member.Role.CUSTOMER);
        BidRequest mine = bidRequest(owner);
        ClientDocument doc = ClientDocument.builder().bidRequest(mine).build();
        UUID docId = UUID.randomUUID();
        given(bidRequestService.findAccessible(mine.getId(), owner)).willReturn(mine);
        given(clientDocumentRepository.findById(docId)).willReturn(Optional.of(doc));

        // when
        clientDocumentService.delete(mine.getId(), docId, owner);

        // then
        then(clientDocumentRepository).should().delete(doc);
    }

    @Test
    @DisplayName("TC-OWN-011 관리자_남의건서류목록_목록")
    void 관리자_list_열림() {
        // given
        Member admin = member(Member.Role.ADMIN);
        BidRequest others = bidRequest(member(Member.Role.CUSTOMER));
        ClientDocument doc = ClientDocument.builder().bidRequest(others).build();
        given(bidRequestService.findAccessible(others.getId(), admin)).willReturn(others);
        given(clientDocumentRepository.findByBidRequestId(others.getId())).willReturn(List.of(doc));

        // when & then
        assertThat(clientDocumentService.list(others.getId(), admin)).containsExactly(doc);
    }

    private static Member member(Member.Role role) {
        Member member = Member.builder().role(role).build();
        ReflectionTestUtils.setField(member, "id", UUID.randomUUID());
        return member;
    }

    private static BidRequest bidRequest(Member owner) {
        BidRequest bidRequest = BidRequest.builder().member(owner).build();
        ReflectionTestUtils.setField(bidRequest, "id", UUID.randomUUID());
        return bidRequest;
    }
}
