package com.biddingagency.domain.event;

/**
 * CR-126: SAM.gov 수집 실패 이벤트.
 *
 * <p>기존 {@link OpportunitiesCollectedEvent}(성공)의 소비자는 타깃 신규 0건이면 발송을 스킵하므로
 * (CR-015 노이즈 차단), "인증 실패로 0건"과 "원래 신규가 없어 0건"이 구분되지 않는다.
 * 실제로 2026-08-03 API 키 만료 후 9일간 수집이 401로 전패했는데 아무 알림도 나가지 않았다.
 * 그래서 실패는 성공 경로와 분리된 본 이벤트로 발행하고, 소비자는 0건 스킵 분기를 타지 않는다.
 *
 * <p>{@code authFailure}가 true면 SAM API 키 만료/무효로 추정되며, 알림 문구가 재발급 절차를 특정한다.
 */
public class CollectionFailedEvent extends DomainEvent {

    private final String keyword;
    private final int errorCount;
    private final Integer lastStatus;
    private final String lastErrorBody;
    private final boolean authFailure;

    public CollectionFailedEvent(String keyword, int errorCount,
                                 Integer lastStatus, String lastErrorBody, boolean authFailure) {
        super("CollectionFailed", null, "CollectorRun", null);
        this.keyword = keyword;
        this.errorCount = errorCount;
        this.lastStatus = lastStatus;
        this.lastErrorBody = lastErrorBody;
        this.authFailure = authFailure;
    }

    public String getKeyword() { return keyword; }
    public int getErrorCount() { return errorCount; }
    public Integer getLastStatus() { return lastStatus; }
    public String getLastErrorBody() { return lastErrorBody; }
    public boolean isAuthFailure() { return authFailure; }
}
