package com.biddingagency.domain.notification.service;

import org.springframework.web.util.HtmlUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * bp-notification 은 넘긴 값을 이스케이프 없이 메일 본문 HTML 에 끼워 넣고, 본문을 항상 HTML 로 보낸다.
 * 고객이 쓴 글이 HTML 로 그려지지 않고 줄바꿈이 남도록 넘기기 전에 바꾼다(CR-132).
 * 제목에도 같은 값이 들어가므로 틀 제목에는 고객이 쓴 값을 넣지 않는다.
 */
public final class MailHtmlValues {

    private MailHtmlValues() {
    }

    /** 값이 비면 빈 문자열로 넘긴다. bp-notification 은 값이 null 인 변수를 치환하지 못한다. */
    public static Map<String, Object> escape(Map<String, ?> values) {
        Map<String, Object> out = new HashMap<>();
        values.forEach((k, v) -> out.put(k, v == null ? "" : text(v.toString())));
        return out;
    }

    static String text(String value) {
        return HtmlUtils.htmlEscape(value, "UTF-8").replace("\r\n", "\n").replace("\n", "<br>");
    }
}
