package com.biddingagency.domain.member.repository;

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

    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Find members by role
     */
    List<Member> findByRole(Member.Role role);

}
