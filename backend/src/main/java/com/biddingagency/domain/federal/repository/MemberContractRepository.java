package com.biddingagency.domain.federal.repository;

import com.biddingagency.domain.federal.entity.MemberContract;
import com.biddingagency.domain.federal.entity.MemberContractSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemberContractRepository extends JpaRepository<MemberContract, UUID> {

    List<MemberContract> findByMemberId(UUID memberId);

    Optional<MemberContract> findByIdAndMemberId(UUID id, UUID memberId);

    boolean existsByMemberIdAndPiid(UUID memberId, String piid);

    List<MemberContract> findBySourceAndFederalContractIdIsNotNull(MemberContractSource source);

    /** 그날 지금 끝날짜나 옵션 끝날짜가 걸린 회원 계약(BID-MKT-033) */
    @Query("SELECT m FROM MemberContract m WHERE m.endDate = :date OR m.potentialEndDate = :date")
    List<MemberContract> findEndingOn(@Param("date") LocalDate date);
}
