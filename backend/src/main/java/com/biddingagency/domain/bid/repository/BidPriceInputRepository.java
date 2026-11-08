package com.biddingagency.domain.bid.repository;

import com.biddingagency.domain.bid.entity.BidPriceInput;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BidPriceInputRepository extends JpaRepository<BidPriceInput, UUID> {

    List<BidPriceInput> findByBidRequestId(UUID bidRequestId);
}
