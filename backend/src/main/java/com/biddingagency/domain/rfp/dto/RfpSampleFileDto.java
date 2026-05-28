package com.biddingagency.domain.rfp.dto;

import com.biddingagency.domain.rfp.entity.RfpSampleFile;
import com.fasterxml.jackson.annotation.JsonInclude;

/** 성공 제안서 원본 파일 DTO (CR-013) */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RfpSampleFileDto(
        String id,
        String fileName,
        Long fileSize,
        String contentType,
        boolean isPws
) {
    public static RfpSampleFileDto from(RfpSampleFile f) {
        return new RfpSampleFileDto(
                f.getId().toString(),
                f.getFileName(),
                f.getFileSize(),
                f.getContentType(),
                f.isPws()
        );
    }
}
