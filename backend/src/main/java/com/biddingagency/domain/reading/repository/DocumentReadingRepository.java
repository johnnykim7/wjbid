package com.biddingagency.domain.reading.repository;

import com.biddingagency.domain.reading.entity.DocumentReading;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentReadingRepository extends JpaRepository<DocumentReading, UUID> {

    Optional<DocumentReading> findByContentSha256(String contentSha256);

    List<DocumentReading> findByContentSha256In(Collection<String> contentSha256s);
}
