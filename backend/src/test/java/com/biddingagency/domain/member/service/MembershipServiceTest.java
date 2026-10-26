package com.biddingagency.domain.member.service;

import com.biddingagency.TestHelper;
import com.biddingagency.domain.member.MembershipError;
import com.biddingagency.domain.member.MembershipException;
import com.biddingagency.domain.member.entity.Member;
import com.biddingagency.domain.member.entity.MemberMembership;
import com.biddingagency.domain.member.repository.MemberMembershipRepository;
import com.biddingagency.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TC-MEM-001 ~ TC-MEM-011 (CR-157): 회원 판정, 회원 기간 넣기와 지우기, 회원 전용 막음(BID-AUTH-008, BID-ADMIN-002).
 * TC-MEM-015 ~ TC-MEM-016 (CR-183): 관리자 회원 상세(BID-ADMIN-002).
 * 저장소는 목록에 담아 흉내 내고, 그날을 품은 기간은 기간 자신의 covers 로 고른다.
 */
@ExtendWith(MockitoExtension.class)
class MembershipServiceTest {

    /** 시험 기준 시각 2026-10-12 10:00 KST */
    private static final Clock NOW = Clock.fixed(Instant.parse("2026-10-12T01:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Mock private MemberMembershipRepository membershipRepository;
    @Mock private MemberRepository memberRepository;

    private final UUID me = UUID.randomUUID();
    private final UUID admin = UUID.randomUUID();
    private final List<MemberMembership> rows = new ArrayList<>();

    @BeforeEach
    void setUp() {
        lenient().when(membershipRepository.findCovering(any(), any())).thenAnswer(inv -> {
            Collection<UUID> ids = inv.getArgument(0);
            LocalDate day = inv.getArgument(1);
            return rows.stream().filter(r -> ids.contains(r.getMemberId()) && r.covers(day)).toList();
        });
    }

    private MembershipService service(Clock clock) {
        return new MembershipService(membershipRepository, memberRepository, clock);
    }

    private MembershipService service() {
        return service(NOW);
    }

    private static Member member(UUID id, Member.Role role) {
        return TestHelper.withId(Member.builder()
                .email(id + "@example.com")
                .companyName("시험 회사")
                .role(role)
                .build(), id);
    }

    private void period(LocalDate startsOn, LocalDate endsOn) {
        rows.add(MemberMembership.builder()
                .id(UUID.randomUUID())
                .memberId(me)
                .startsOn(startsOn)
                .endsOn(endsOn)
                .createdBy(admin)
                .createdAt(LocalDateTime.of(2026, 10, 1, 9, 0))
                .build());
    }

    private static LocalDate oct(int day) {
        return LocalDate.of(2026, 10, day);
    }

    @Test
    @DisplayName("TC-MEM-001 기간안_시작일끝일당일포함_회원")
    void 기간안_시작일끝일당일포함_회원() {
        period(oct(12), oct(12));

        assertThat(service().status(member(me, Member.Role.CUSTOMER)).member()).isTrue();
    }

    @Test
    @DisplayName("TC-MEM-002 시작전_무료회원")
    void 시작전_무료회원() {
        period(oct(13), oct(31));

        assertThat(service().status(member(me, Member.Role.CUSTOMER)).member()).isFalse();
    }

    @Test
    @DisplayName("TC-MEM-003 끝난뒤_무료회원")
    void 끝난뒤_무료회원() {
        period(oct(1), oct(11));

        assertThat(service().status(member(me, Member.Role.CUSTOMER)).member()).isFalse();
    }

    @Test
    @DisplayName("TC-MEM-004 관리자_기간없음_회원")
    void 관리자_기간없음_회원() {
        assertThat(service().status(member(admin, Member.Role.ADMIN)))
                .isEqualTo(new MembershipService.Status(true, null));
    }

    @Test
    @DisplayName("TC-MEM-005 서버시간대UTC_서울날짜로가름")
    void 서버시간대UTC_서울날짜로가름() {
        // UTC 로는 10/12 15:30 이지만 서울은 10/13 00:30 이다
        Clock utc = Clock.fixed(Instant.parse("2026-10-12T15:30:00Z"), ZoneOffset.UTC);
        period(oct(13), oct(31));

        assertThat(service(utc).status(member(me, Member.Role.CUSTOMER)).member()).isTrue();
    }

    @Test
    @DisplayName("TC-MEM-006 기간겹침_가장늦은끝일")
    void 기간겹침_가장늦은끝일() {
        period(oct(1), oct(20));
        period(oct(10), LocalDate.of(2026, 11, 30));

        assertThat(service().status(member(me, Member.Role.CUSTOMER)).endsOn()).isEqualTo(LocalDate.of(2026, 11, 30));
    }

    @Test
    @DisplayName("TC-MEM-007 끝일이시작일앞_MEMBERSHIP_PERIOD_INVALID")
    void 끝일이시작일앞_MEMBERSHIP_PERIOD_INVALID() {
        MembershipException ex = catchThrowableOfType(
                () -> service().grant(me, oct(20), oct(10), null, admin), MembershipException.class);

        assertThat(ex.getError()).isEqualTo(MembershipError.MEMBERSHIP_PERIOD_INVALID);
        verify(membershipRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-MEM-008 넣기_넣은관리자와메모저장")
    void 넣기_넣은관리자와메모저장() {
        when(memberRepository.existsById(me)).thenReturn(true);
        when(memberRepository.findById(admin)).thenReturn(Optional.of(member(admin, Member.Role.ADMIN)));
        when(membershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        ArgumentCaptor<MemberMembership> saved = ArgumentCaptor.forClass(MemberMembership.class);

        service().grant(me, oct(12), LocalDate.of(2026, 11, 11), "입금 확인", admin);

        verify(membershipRepository).save(saved.capture());
        assertThat(saved.getValue())
                .extracting(MemberMembership::getMemberId, MemberMembership::getStartsOn, MemberMembership::getEndsOn,
                        MemberMembership::getMemo, MemberMembership::getCreatedBy)
                .containsExactly(me, oct(12), LocalDate.of(2026, 11, 11), "입금 확인", admin);
    }

    @Test
    @DisplayName("TC-MEM-009 다른고객의기간지우기_MEMBERSHIP_NOT_FOUND")
    void 다른고객의기간지우기_MEMBERSHIP_NOT_FOUND() {
        UUID other = UUID.randomUUID();
        UUID rowOfMe = UUID.randomUUID();
        when(membershipRepository.findByIdAndMemberId(rowOfMe, other)).thenReturn(Optional.empty());

        MembershipException ex = catchThrowableOfType(
                () -> service().delete(other, rowOfMe), MembershipException.class);

        assertThat(ex.getError()).isEqualTo(MembershipError.MEMBERSHIP_NOT_FOUND);
        verify(membershipRepository, never()).delete(any());
    }

    @Test
    @DisplayName("TC-MEM-010 무료회원_requireMember_MEMBERSHIP_REQUIRED")
    void 무료회원_requireMember_MEMBERSHIP_REQUIRED() {
        when(memberRepository.findById(me)).thenReturn(Optional.of(member(me, Member.Role.CUSTOMER)));

        MembershipException ex = catchThrowableOfType(
                () -> service().requireMember(me), MembershipException.class);

        assertThat(ex.getError()).isEqualTo(MembershipError.MEMBERSHIP_REQUIRED);
    }

    @Test
    @DisplayName("TC-MEM-011 로그인안함_requireMember_LOGIN_REQUIRED")
    void 로그인안함_requireMember_LOGIN_REQUIRED() {
        MembershipException ex = catchThrowableOfType(
                () -> service().requireMember(null), MembershipException.class);

        assertThat(ex.getError()).isEqualTo(MembershipError.LOGIN_REQUIRED);
    }

    @Test
    @DisplayName("TC-MEM-015 상세_가입정보와지금기간끝일_그대로")
    void 상세_가입정보와지금기간끝일_그대로() {
        Member customer = TestHelper.withId(Member.builder()
                .email("customer@example.com")
                .emailVerifiedAt(LocalDateTime.of(2026, 10, 1, 9, 30))
                .companyName("시험 회사")
                .businessRegistrationNumber("123-45-67890")
                .phone("010-1234-5678")
                .address("서울시 중구")
                .role(Member.Role.CUSTOMER)
                .build(), me);
        when(memberRepository.findById(me)).thenReturn(Optional.of(customer));
        period(oct(1), LocalDate.of(2026, 11, 30));

        assertThat(service().detail(me)).isEqualTo(new MembershipService.Detail(
                customer, new MembershipService.Status(true, LocalDate.of(2026, 11, 30))));
    }

    @Test
    @DisplayName("TC-MEM-016 상세_없는회원_MEMBER_NOT_FOUND")
    void 상세_없는회원_MEMBER_NOT_FOUND() {
        when(memberRepository.findById(me)).thenReturn(Optional.empty());

        MembershipException ex = catchThrowableOfType(
                () -> service().detail(me), MembershipException.class);

        assertThat(ex.getError()).isEqualTo(MembershipError.MEMBER_NOT_FOUND);
    }
}
