package com.biddingagency.domain.compliance.entity;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * 형식 판정에서 기계가 읽는 파일 형식 이름 (CR-141, BIZ-047).
 * names 는 공고 조건 원문에서 그 형식을 가리키는 낱말, extensions 는 올린 파일 확장자다.
 */
public enum FileFormat {
    PDF(Set.of("pdf"), Set.of("pdf")),
    WORD(Set.of("word", "doc", "docx"), Set.of("doc", "docx")),
    EXCEL(Set.of("excel", "xls", "xlsx"), Set.of("xls", "xlsx")),
    HWP(Set.of("hwp"), Set.of("hwp")),
    DWG(Set.of("dwg"), Set.of("dwg"));

    private final Set<String> names;
    private final Set<String> extensions;

    FileFormat(Set<String> names, Set<String> extensions) {
        this.names = names;
        this.extensions = extensions;
    }

    public static Optional<FileFormat> byName(String word) {
        String lower = word.toLowerCase(Locale.ROOT);
        for (FileFormat format : values()) {
            if (format.names.contains(lower)) {
                return Optional.of(format);
            }
        }
        return Optional.empty();
    }

    /** 파일 이름의 마지막 점 뒤(소문자). 점이 없으면 null */
    public static String extensionOf(String fileName) {
        if (fileName == null) {
            return null;
        }
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return null;
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    public static Optional<FileFormat> byExtension(String extension) {
        if (extension == null) {
            return Optional.empty();
        }
        String lower = extension.toLowerCase(Locale.ROOT);
        for (FileFormat format : values()) {
            if (format.extensions.contains(lower)) {
                return Optional.of(format);
            }
        }
        return Optional.empty();
    }
}
