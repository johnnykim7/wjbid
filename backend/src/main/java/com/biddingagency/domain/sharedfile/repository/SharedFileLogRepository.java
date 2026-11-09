package com.biddingagency.domain.sharedfile.repository;

import com.biddingagency.domain.sharedfile.entity.SharedFileLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SharedFileLogRepository extends JpaRepository<SharedFileLog, Long> {

    Page<SharedFileLog> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);
}
