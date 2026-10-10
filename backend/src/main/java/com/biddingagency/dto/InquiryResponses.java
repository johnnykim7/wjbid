package com.biddingagency.dto;

/**
 * 1:1 문의 응답 모양 (CR-132, T3-2). 시각은 다른 관리자 화면과 같은 "yyyy-MM-dd HH:mm" 문자열이다.
 */
public final class InquiryResponses {

    private InquiryResponses() {
    }

    public record TypeItem(String code, String label) {
    }

    public record Submitted(String id, String status, String createdAt) {
    }

    public record ListItem(String id, String createdAt, String typeCode, String typeLabel, String title,
                           String name, String email, boolean member, String status) {
    }

    public record Counts(long all, long received, long answered) {
    }

    public record AdminNotice(long total, long success, String lastSentAt) {
    }

    public record Detail(String id, String typeCode, String typeLabel, String title, String content,
                         String name, String email, String memberId, String memberCompanyName,
                         String status, String createdAt, AdminNotice adminNotice,
                         String answer, String answeredAt, String answeredByEmail) {
    }
}
