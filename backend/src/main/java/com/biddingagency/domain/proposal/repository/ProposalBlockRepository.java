package com.biddingagency.domain.proposal.repository;

import com.biddingagency.domain.proposal.entity.ProposalBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProposalBlockRepository extends JpaRepository<ProposalBlock, UUID> {

    List<ProposalBlock> findBySectionIdOrderByOrderNoAsc(UUID sectionId);

    /** 부분 재생성 시 해당 section 의 기존 block 전량 교체 */
    void deleteBySectionId(UUID sectionId);
}
