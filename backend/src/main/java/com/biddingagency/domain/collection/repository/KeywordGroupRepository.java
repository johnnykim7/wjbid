package com.biddingagency.domain.collection.repository;

import com.biddingagency.domain.collection.entity.KeywordGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface KeywordGroupRepository extends JpaRepository<KeywordGroup, UUID> {

    List<KeywordGroup> findByActiveTrue();
}
