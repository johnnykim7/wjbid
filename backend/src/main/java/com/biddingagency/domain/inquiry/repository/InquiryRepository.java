package com.biddingagency.domain.inquiry.repository;

import com.biddingagency.domain.inquiry.entity.Inquiry;
import com.biddingagency.domain.inquiry.entity.InquiryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

public interface InquiryRepository extends JpaRepository<Inquiry, UUID> {

    @Query("SELECT i FROM Inquiry i WHERE (:status IS NULL OR i.status = :status) "
            + "AND (:typeCode IS NULL OR i.typeCode = :typeCode)")
    Page<Inquiry> search(@Param("status") InquiryStatus status,
                         @Param("typeCode") String typeCode,
                         Pageable pageable);

    long countByStatus(InquiryStatus status);

    /**
     * 미답변 문의에만 답변을 채운다. 0 이 돌아오면 그 사이 다른 관리자가 먼저 답했다(BIZ-024).
     */
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Inquiry i SET i.status = com.biddingagency.domain.inquiry.entity.InquiryStatus.ANSWERED, "
            + "i.answer = :answer, i.answeredAt = :answeredAt, i.answeredBy = :answeredBy, i.updatedAt = :answeredAt "
            + "WHERE i.id = :id AND i.status = com.biddingagency.domain.inquiry.entity.InquiryStatus.RECEIVED")
    int markAnswered(@Param("id") UUID id,
                     @Param("answer") String answer,
                     @Param("answeredAt") LocalDateTime answeredAt,
                     @Param("answeredBy") UUID answeredBy);
}
