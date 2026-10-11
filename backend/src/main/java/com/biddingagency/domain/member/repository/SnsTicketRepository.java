package com.biddingagency.domain.member.repository;

import com.biddingagency.domain.member.entity.SnsTicket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SNS 표 저장소 (CR-146)
 */
public interface SnsTicketRepository extends JpaRepository<SnsTicket, UUID> {

    Optional<SnsTicket> findByTokenHash(String tokenHash);
}
