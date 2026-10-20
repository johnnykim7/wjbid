package com.biddingagency.domain.opportunity.dto;

import com.biddingagency.domain.opportunity.entity.OpportunityAttachment;
import com.biddingagency.domain.opportunity.service.AttachmentFiles;
import com.biddingagency.domain.opportunity.service.PieeFetchService;

import java.util.UUID;

/**
 * 고객 공고 화면 첨부 칸의 받아 둔 첨부 한 줄 (CR-168, BIZ-084).
 * 저장 위치, 실패 사유, 내용 해시는 고객에게 주지 않는다. 원문 주소는 SAM.gov 첨부만 준다.
 */
public record CustomerAttachmentDto(
        UUID id,
        String fileName,
        Long fileSize,
        String contentType,
        Source source,
        String sourceUrl) {

    public enum Source { SAM, PIEE, UPLOAD }

    public static CustomerAttachmentDto from(OpportunityAttachment a) {
        Source source = sourceOf(a.getSourceUrl());
        return new CustomerAttachmentDto(a.getId(), a.getFileName(), a.getFileSize(), a.getContentType(),
                source, source == Source.SAM ? a.getSourceUrl() : null);
    }

    static Source sourceOf(String sourceUrl) {
        if (sourceUrl == null || AttachmentFiles.ADMIN_UPLOAD_SOURCE.equals(sourceUrl)) return Source.UPLOAD;
        if (sourceUrl.startsWith(PieeFetchService.SOURCE_PREFIX)) return Source.PIEE;
        return Source.SAM;
    }
}
