package com.biddingagency.domain.compliance.repository;

import com.biddingagency.domain.compliance.entity.SubmissionCheckNote;
import com.biddingagency.domain.compliance.entity.SubmissionNoteTarget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubmissionCheckNoteRepository extends JpaRepository<SubmissionCheckNote, UUID> {

    List<SubmissionCheckNote> findByBidRequestId(UUID bidRequestId);

    Optional<SubmissionCheckNote> findByBidRequestIdAndTargetTypeAndTargetKey(
            UUID bidRequestId, SubmissionNoteTarget targetType, String targetKey);
}
