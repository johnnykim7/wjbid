package com.biddingagency.domain.member.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.biddingagency.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Member repository
 */
@Repository
public interface MemberRepository extends JpaRepository<Member, UUID> {

    /**
     * Find member by email
     */
    Optional<Member> findByEmail(String email);

    // 동시에 틀려도 횟수가 빠지지 않도록 행을 잡고 센다
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Member m where m.email = :email")
    Optional<Member> findByEmailForUpdate(@Param("email") String email);

    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Find members by role
     */
    List<Member> findByRole(Member.Role role);

}
