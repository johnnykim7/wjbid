package com.biddingagency.domain.compliance.service;

import com.biddingagency.domain.compliance.entity.FileFormat;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;

/**
 * 올린 PDF 의 쪽수를 센다 (CR-141, BIZ-048). PDF 가 아니거나 읽지 못하면 null 이고, 판정은 사람 확인이 된다.
 */
@Slf4j
@Component
public class PdfPageCounter {

    public Integer count(String fileName, byte[] content) {
        if (FileFormat.byExtension(FileFormat.extensionOf(fileName)).orElse(null) != FileFormat.PDF
                || content == null || content.length == 0) {
            return null;
        }
        PdfReader reader = null;
        try {
            // 열기 암호 없이 권한만 걸린 PDF 도 쪽수는 읽는다
            reader = new PdfReader(new ByteArrayInputStream(content)).setUnethicalReading(true);
            try (PdfDocument pdf = new PdfDocument(reader)) {
                return pdf.getNumberOfPages();
            }
        } catch (Exception e) {
            log.warn("[CR-141] PDF 쪽수를 세지 못함: file={}, reason={}", fileName, e.getMessage());
            return null;
        }
    }
}
