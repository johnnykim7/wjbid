package com.biddingagency.dto;

import com.biddingagency.domain.bid.entity.BidRequestState;
import com.biddingagency.domain.compliance.entity.FileFormat;
import com.biddingagency.domain.compliance.entity.PreCheckVerdict;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 제출 전 검사 응답 (CR-141, T3-2 G-2)
 */
public final class PreCheckResponses {

    private PreCheckResponses() {
    }

    /** GET /bid-requests/my/pre-check 한 줄. analysisReady 가 false 면 summary 는 null */
    public record Row(UUID bidRequestId, BidRequestState state, String solicitationNumber, String title,
                      String titleKo, OffsetDateTime responseDeadline, boolean analysisReady, Summary summary) {
    }

    /**
     * GET /bid-requests/{id}/pre-check. editable 이 false 면 올리기, 고르기, 바꾸기, 빼기를 받지 않는다(409).
     * maxUploadMb 는 올릴 수 있는 파일 한도(spring.servlet.multipart.max-file-size)
     */
    public record Detail(UUID bidRequestId, BidRequestState state, String solicitationNumber, String title,
                         String titleKo, OffsetDateTime responseDeadline, UUID noticeId, boolean analysisReady,
                         List<ClientItem> clientItems, List<PlatformItem> platformItems, Summary summary,
                         boolean canTransition, boolean editable, long maxUploadMb) {
    }

    /**
     * required 는 필수 고객 올림 항목 수, uploaded 는 그중 이어진 수, missing 은 그 차.
     * mismatch, needsHuman 은 이어진 항목만 센다.
     */
    public record Summary(int required, int uploaded, int missing, int mismatch, int needsHuman) {
    }

    public enum Presence { UPLOADED, MISSING }

    public record ClientItem(UUID requirementItemId, String title, String description, boolean mandatory,
                             boolean isBlocker, String sourceRef, String notes, String formatText,
                             String pageLimitText, Document document, Presence presence,
                             FormatJudgement format, PageJudgement pages) {
    }

    public record Document(UUID clientDocumentId, String fileName, String extension, Integer pageCount,
                           Long fileSize, OffsetDateTime uploadedAt) {
    }

    /** basis 는 공고 조건 원문 그대로, allowed 는 원문에서 읽어 낸 형식(읽지 못하면 빈 배열) */
    public record FormatJudgement(PreCheckVerdict verdict, String basis, List<FileFormat> allowed) {
    }

    /** basis 는 공고 조건 원문 그대로, limit 은 원문에서 읽어 낸 쪽수 한도(읽지 못하면 null) */
    public record PageJudgement(PreCheckVerdict verdict, String basis, Integer limit) {
    }

    public record PlatformItem(UUID requirementItemId, String title, String fulfillmentParty, String sourceRef) {
    }
}
