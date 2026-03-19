package com.biddingagency.domain.bookmark.service;

import com.biddingagency.domain.bookmark.entity.Bookmark;
import com.biddingagency.domain.bookmark.repository.BookmarkRepository;
import com.biddingagency.domain.member.entity.Member;
import com.biddingagency.domain.member.repository.MemberRepository;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.repository.OpportunityRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * TC-BM-001 ~ TC-BM-003: BookmarkService 테스트
 */
@ExtendWith(MockitoExtension.class)
class BookmarkServiceTest {

    @Mock
    private BookmarkRepository bookmarkRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private OpportunityRepository opportunityRepository;

    @InjectMocks
    private BookmarkService bookmarkService;

    // TC-BM-001: 북마크 추가
    @Test
    @DisplayName("유효한멤버공고_북마크추가_Bookmark생성")
    void 유효입력_add_북마크생성() {
        // given
        UUID memberId = UUID.randomUUID();
        UUID opportunityId = UUID.randomUUID();
        given(bookmarkRepository.existsByMemberIdAndOpportunityId(memberId, opportunityId)).willReturn(false);
        given(memberRepository.findById(memberId)).willReturn(Optional.of(
                Member.builder().email("test@test.com").passwordHash("hash").companyName("Co").build()));
        given(opportunityRepository.findById(opportunityId)).willReturn(Optional.of(
                Opportunity.builder().noticeId("N-1").title("T").active(true)
                        .firstSeenAt(java.time.LocalDateTime.now()).lastModifiedAt(java.time.LocalDateTime.now()).build()));

        // when
        bookmarkService.add(memberId, opportunityId);

        // then
        then(bookmarkRepository).should().save(any(Bookmark.class));
    }

    // TC-BM-002: 중복 북마크 → 멱등성 (무시)
    @Test
    @DisplayName("이미북마크된공고_추가시도_무시_멱등성")
    void 중복북마크_add_무시() {
        // given
        UUID memberId = UUID.randomUUID();
        UUID opportunityId = UUID.randomUUID();
        given(bookmarkRepository.existsByMemberIdAndOpportunityId(memberId, opportunityId)).willReturn(true);

        // when
        bookmarkService.add(memberId, opportunityId);

        // then
        then(bookmarkRepository).should(never()).save(any());
    }

    // TC-BM-003: 북마크 삭제
    @Test
    @DisplayName("기존북마크_삭제_deleteByMemberAndOpportunity호출")
    void 기존북마크_remove_삭제() {
        // given
        UUID memberId = UUID.randomUUID();
        UUID opportunityId = UUID.randomUUID();

        // when
        bookmarkService.remove(memberId, opportunityId);

        // then
        then(bookmarkRepository).should().deleteByMemberIdAndOpportunityId(memberId, opportunityId);
    }

    // 북마크 상태 확인
    @Test
    @DisplayName("북마크여부확인_true반환")
    void 북마크됨_isBookmarked_true() {
        // given
        UUID memberId = UUID.randomUUID();
        UUID opportunityId = UUID.randomUUID();
        given(bookmarkRepository.existsByMemberIdAndOpportunityId(memberId, opportunityId)).willReturn(true);

        // when & then
        assertThat(bookmarkService.isBookmarked(memberId, opportunityId)).isTrue();
    }
}
