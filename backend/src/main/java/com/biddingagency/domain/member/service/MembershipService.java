package com.biddingagency.domain.member.service;

import com.biddingagency.domain.member.MembershipError;
import com.biddingagency.domain.member.MembershipException;
import com.biddingagency.domain.member.entity.Member;
import com.biddingagency.domain.member.entity.MemberMembership;
import com.biddingagency.domain.member.repository.MemberMembershipRepository;
import com.biddingagency.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 회원 구분 (CR-157, BID-AUTH-008).
 * 오늘(서울 날짜)이 회원 기간 하나라도 안이면 회원이고, 관리자는 기간 없이 늘 회원과 같다(BIZ-072).
 * 공고문 내용을 내주는 곳은 모두 requireMember 를 거친다(BIZ-071).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MembershipService {

    // 서버 시간대와 상관없이 서울 날짜로 가른다. 서류 만료와 같은 기준이다
    private static final ZoneId SERVICE_ZONE = ZoneId.of(DocumentExpiryService.SERVICE_ZONE);

    private final MemberMembershipRepository membershipRepository;
    private final MemberRepository memberRepository;
    private final Clock clock;

    /** endsOn 은 오늘을 품은 기간 중 가장 늦은 끝일. 관리자와 무료회원은 null */
    public record Status(boolean member, LocalDate endsOn) {
    }

    public record Line(UUID id, LocalDate startsOn, LocalDate endsOn, String memo,
                       String createdByEmail, LocalDateTime createdAt, boolean current) {
    }

    public Status status(Member member) {
        if (member.isAdmin()) {
            return new Status(true, null);
        }
        LocalDate endsOn = currentEndsOn(List.of(member.getId())).get(member.getId());
        return new Status(endsOn != null, endsOn);
    }

    /** 로그인하지 않았으면 401. 회원 구분은 보지 않는다 (BIZ-084) */
    public void requireLogin(UUID memberId) {
        if (memberId == null) {
            throw new MembershipException(MembershipError.LOGIN_REQUIRED);
        }
    }

    /** 로그인하지 않았으면 401, 로그인한 무료회원이면 403 (BIZ-071) */
    public void requireMember(UUID memberId) {
        requireLogin(memberId);
        if (!membershipRepository.findCovering(List.of(memberId), today()).isEmpty()) {
            return;
        }
        boolean admin = memberRepository.findById(memberId).map(Member::isAdmin).orElse(false);
        if (!admin) {
            throw new MembershipException(MembershipError.MEMBERSHIP_REQUIRED);
        }
    }

    /** 회원 번호별 지금 기간의 끝일. 기간 밖인 회원은 담지 않는다 */
    public Map<UUID, LocalDate> currentEndsOn(Collection<UUID> memberIds) {
        if (memberIds.isEmpty()) {
            return Map.of();
        }
        return membershipRepository.findCovering(memberIds, today()).stream()
                .collect(Collectors.toMap(MemberMembership::getMemberId, MemberMembership::getEndsOn,
                        (a, b) -> a.isAfter(b) ? a : b));
    }

    public List<Line> history(UUID memberId) {
        requireExisting(memberId);
        List<MemberMembership> rows = membershipRepository.findByMemberIdOrderByStartsOnDescCreatedAtDesc(memberId);
        Map<UUID, String> emails = memberRepository
                .findAllById(rows.stream().map(MemberMembership::getCreatedBy).distinct().toList()).stream()
                .collect(Collectors.toMap(Member::getId, Member::getEmail));
        LocalDate today = today();
        return rows.stream().map(r -> line(r, emails.get(r.getCreatedBy()), today)).toList();
    }

    @Transactional
    public Line grant(UUID memberId, LocalDate startsOn, LocalDate endsOn, String memo, UUID adminId) {
        if (startsOn == null || endsOn == null || endsOn.isBefore(startsOn)) {
            throw new MembershipException(MembershipError.MEMBERSHIP_PERIOD_INVALID);
        }
        requireExisting(memberId);
        MemberMembership saved = membershipRepository.save(MemberMembership.builder()
                .memberId(memberId)
                .startsOn(startsOn)
                .endsOn(endsOn)
                .memo(memo == null || memo.isBlank() ? null : memo.trim())
                .createdBy(adminId)
                .createdAt(LocalDateTime.now(clock))
                .build());
        String adminEmail = memberRepository.findById(adminId).map(Member::getEmail).orElse(null);
        return line(saved, adminEmail, today());
    }

    @Transactional
    public void delete(UUID memberId, UUID membershipId) {
        MemberMembership row = membershipRepository.findByIdAndMemberId(membershipId, memberId)
                .orElseThrow(() -> new MembershipException(MembershipError.MEMBERSHIP_NOT_FOUND));
        membershipRepository.delete(row);
    }

    private void requireExisting(UUID memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new MembershipException(MembershipError.MEMBER_NOT_FOUND);
        }
    }

    private static Line line(MemberMembership r, String createdByEmail, LocalDate today) {
        return new Line(r.getId(), r.getStartsOn(), r.getEndsOn(), r.getMemo(), createdByEmail,
                r.getCreatedAt(), r.covers(today));
    }

    private LocalDate today() {
        return LocalDate.now(clock.withZone(SERVICE_ZONE));
    }
}
