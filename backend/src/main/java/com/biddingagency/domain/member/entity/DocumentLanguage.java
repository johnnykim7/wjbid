package com.biddingagency.domain.member.entity;

/**
 * 국문·영문 칸에 올린 파일의 언어 (CR-147, BIZ-065).
 * label 은 그 칸 서류 제목 뒤에 붙여 서류함에서 고를 때 두 파일을 가른다.
 */
public enum DocumentLanguage {

    KO("국문"),
    EN("영문");

    private final String label;

    DocumentLanguage(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
