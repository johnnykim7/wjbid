package com.biddingagency.domain.reading.entity;

/**
 * 고객 서류를 Aimbase 가 읽는 꼴 (CR-169, BIZ-086).
 * PDF 는 PDF 와 그림(한 쪽 PDF 로 감싼다), TEXT 는 DOCX, XLSX, PPTX(글자만 뽑는다)와 TXT, CSV, MD,
 * HWP 는 HWP, HWPX(작업장에서 MD 로 바꾼다), NONE 은 읽지 않는 형식이다.
 */
public enum ReadAs {
    PDF,
    TEXT,
    HWP,
    NONE
}
