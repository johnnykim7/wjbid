package com.biddingagency.domain.member.repository;

import com.biddingagency.domain.member.entity.MemberMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemberMembershipRepository extends JpaRepository<MemberMembership, UUID> {

    /** 그날을 품은 기간. 두 날 모두 포함한다 (BIZ-072) */
    @Query("SELECT m FROM MemberMembership m WHERE m.memberId IN :memberIds AND m.startsOn <= :day AND m.endsOn >= :day")
    List<MemberMembership> findCovering(@Param("memberIds") Collection<UUID> memberIds, @Param("day") LocalDate day);

    List<MemberMembership> findByMemberIdOrderByStartsOnDescCreatedAtDesc(UUID memberId);

    Optional<MemberMembership> findByIdAndMemberId(UUID id, UUID memberId);
}
