package com.biddingagency.domain.bookmark.service;

import com.biddingagency.domain.bookmark.entity.Bookmark;
import com.biddingagency.domain.bookmark.repository.BookmarkRepository;
import com.biddingagency.domain.member.entity.Member;
import com.biddingagency.domain.member.repository.MemberRepository;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.repository.OpportunityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final MemberRepository memberRepository;
    private final OpportunityRepository opportunityRepository;

    public Page<Bookmark> findByMember(UUID memberId, Pageable pageable) {
        return bookmarkRepository.findByMemberIdWithOpportunity(memberId, pageable);
    }

    public boolean isBookmarked(UUID memberId, UUID opportunityId) {
        return bookmarkRepository.existsByMemberIdAndOpportunityId(memberId, opportunityId);
    }

    @Transactional
    public void add(UUID memberId, UUID opportunityId) {
        if (bookmarkRepository.existsByMemberIdAndOpportunityId(memberId, opportunityId)) {
            return; // 이미 북마크됨 - 멱등성
        }
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));
        Opportunity opportunity = opportunityRepository.findById(opportunityId)
                .orElseThrow(() -> new IllegalArgumentException("Opportunity not found"));
        bookmarkRepository.save(Bookmark.builder().member(member).opportunity(opportunity).build());
    }

    @Transactional
    public void remove(UUID memberId, UUID opportunityId) {
        bookmarkRepository.deleteByMemberIdAndOpportunityId(memberId, opportunityId);
    }
}
