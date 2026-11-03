package com.biddingagency.common;

import java.util.Locale;
import java.util.regex.Pattern;

/** 검색어와 찾을 글을 같은 꼴로 맞춘다. 띄어쓰기와 대소문자를 무시한다(BIZ-098) */
public final class SearchText {

    // 붙여넣은 글의 줄 바꿈 없는 공백과 전각 공백도 띄어쓰기로 본다
    private static final Pattern SPACES = Pattern.compile("(?U)\\s+");

    private SearchText() {
    }

    public static String key(String s) {
        return s == null ? "" : SPACES.matcher(s).replaceAll("").toLowerCase(Locale.ROOT);
    }

    /** key 로 맞춘 needle 이 value 안에 있는가. needle 이 비었으면 걸리지 않는다 */
    public static boolean contains(String value, String needleKey) {
        return value != null && !needleKey.isEmpty() && key(value).contains(needleKey);
    }
}
