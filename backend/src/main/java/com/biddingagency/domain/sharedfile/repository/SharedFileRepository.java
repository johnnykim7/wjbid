package com.biddingagency.domain.sharedfile.repository;

import com.biddingagency.domain.sharedfile.entity.SharedFile;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SharedFileRepository extends JpaRepository<SharedFile, UUID> {

    Page<SharedFile> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // 동시에 틀려도 횟수가 빠지지 않도록 행을 잡고 센다
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from SharedFile f where f.id = :id")
    Optional<SharedFile> findByIdForUpdate(@Param("id") UUID id);
}
