package com.biddingagency.domain.bid.repository;

import com.biddingagency.domain.bid.entity.ClientDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClientDocumentRepository extends JpaRepository<ClientDocument, UUID> {

    List<ClientDocument> findByBidRequestId(UUID bidRequestId);

    List<ClientDocument> findByMemberId(UUID memberId);

    long countByBidRequestId(UUID bidRequestId);
}
