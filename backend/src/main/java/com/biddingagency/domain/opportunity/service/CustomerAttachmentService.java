package com.biddingagency.domain.opportunity.service;

import com.biddingagency.domain.member.service.MembershipService;
import com.biddingagency.domain.opportunity.dto.CustomerAttachmentDto;
import com.biddingagency.domain.opportunity.entity.AttachmentDownloadStatus;
import com.biddingagency.domain.opportunity.entity.OpportunityAttachment;
import com.biddingagency.domain.opportunity.repository.OpportunityAttachmentRepository;
import com.biddingagency.domain.opportunity.repository.OpportunityRepository;
import com.biddingagency.integration.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 고객 공고 화면 첨부 칸의 받아 둔 첨부 목록과 내려받기 (CR-168, BIZ-084).
 * 공고 원문 첨부라 회원 구분 없이 로그인만 본다. 공고문 내용은 아니어서 requireMember 를 쓰지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerAttachmentService {

    private final OpportunityRepository opportunityRepository;
    private final OpportunityAttachmentRepository attachmentRepository;
    private final StorageService storageService;
    private final MembershipService membershipService;

    public record Download(byte[] content, String fileName, String contentType) {
    }

    public List<CustomerAttachmentDto> list(UUID opportunityId) {
        if (!opportunityRepository.existsById(opportunityId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return attachmentRepository.findByOpportunityId(opportunityId).stream()
                .filter(CustomerAttachmentService::stored)
                .sorted(Comparator.comparing(OpportunityAttachment::getFileName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(CustomerAttachmentDto::from)
                .toList();
    }

    /** 다른 공고의 첨부와 저장되지 않은 첨부는 없는 첨부와 같이 비어 돌아간다 */
    public Optional<Download> load(UUID memberId, UUID opportunityId, UUID attachmentId) {
        membershipService.requireLogin(memberId);
        return attachmentRepository.findById(attachmentId)
                .filter(a -> a.getOpportunity().getId().equals(opportunityId))
                .filter(CustomerAttachmentService::stored)
                .map(a -> new Download(storageService.load(a.getStorageUrl()), a.getFileName(), a.getContentType()));
    }

    private static boolean stored(OpportunityAttachment a) {
        return a.getDownloadStatus() == AttachmentDownloadStatus.SUCCESS
                && a.getStorageUrl() != null && !a.getStorageUrl().isBlank();
    }
}
