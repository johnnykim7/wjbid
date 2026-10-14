package com.biddingagency.domain.notification.service;

import com.biddingagency.domain.bid.entity.BidRequest;
import com.biddingagency.domain.bid.entity.BidRequestState;
import com.biddingagency.domain.bid.repository.BidRequestRepository;
import com.biddingagency.domain.event.BidRequestCreatedEvent;
import com.biddingagency.domain.event.CollectionFailedEvent;
import com.biddingagency.domain.event.BidRequestStateChangedEvent;
import com.biddingagency.domain.event.DeadlineApproachingEvent;
import com.biddingagency.domain.event.DocumentGeneratedEvent;
import com.biddingagency.domain.event.OpportunitiesCollectedEvent;
import com.biddingagency.domain.event.OpportunityAnalysisCompletedEvent;
import com.biddingagency.domain.event.OpportunityApprovedEvent;
import com.biddingagency.domain.member.entity.Member;
import com.biddingagency.domain.member.repository.MemberRepository;
import com.biddingagency.domain.member.service.MembershipService;
import com.biddingagency.domain.notification.entity.NotificationType;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.repository.OpportunityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 도메인 이벤트 → bp-notification 알림 발송 (CR-005/006).
 *
 * 관리자 알림(수집/신청/문서/분석/마감) + 고객 알림(신규 공고 등록).
 * 각 핸들러는 해당 bp-notification 템플릿이 요구하는 변수를 채워 발송한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    /** CR-126 멱등키 일자 기준. 관리자가 보는 날짜(KST)와 일치시킨다. */
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final NotificationService notificationService;
    private final MemberRepository memberRepository;
    private final OpportunityRepository opportunityRepository;
    private final BidRequestRepository bidRequestRepository;
    private final MembershipService membershipService;

    @Value("${app.customer-portal-url:http://localhost:3183}")
    private String customerPortalUrl;

    /** 회원 표시명: 담당자명 우선, 없으면 회사명, 그것도 없으면 이메일. */
    private String displayName(Member m) {
        if (m.getContactPerson() != null && !m.getContactPerson().isBlank()) return m.getContactPerson();
        if (m.getCompanyName() != null && !m.getCompanyName().isBlank()) return m.getCompanyName();
        return m.getEmail();
    }

    @Async
    @EventListener
    public void onOpportunitiesCollected(OpportunitiesCollectedEvent event) {
        // CR-015: 시설관리 타깃(IndustryClassifier 매칭) 신규가 0이면 메일 스킵.
        // q=korea 전문검색이 끌어오는 무관 부품조달만 신규일 때 노이즈 메일을 막는다.
        if (event.getTargetNewCount() == 0) {
            log.info("Skipping COLLECTION_COMPLETE notification: 0 target new opportunities "
                            + "(totalNew={}, changed={})",
                    event.getNewCount(), event.getUpdatedCount());
            return;
        }
        Map<String, Object> vars = Map.of(
                "newCount", String.valueOf(event.getTargetNewCount()),
                "totalNewCount", String.valueOf(event.getNewCount()),
                "updateCount", String.valueOf(event.getUpdatedCount()));
        for (Member admin : memberRepository.findByRole(Member.Role.ADMIN)) {
            notificationService.sendNotification(
                    NotificationType.COLLECTION_COMPLETE,
                    admin.getEmail(), admin.getId(),
                    "[Bidding Agency] 공고 수집 완료", vars,
                    event.getEntityId(), "CollectorRun",
                    "COLLECTION_" + event.getEntityId() + "_" + admin.getId());
        }
    }

    /**
     * CR-126: 수집 실패 관리자 통보.
     *
     * <p>{@link #onOpportunitiesCollected}의 0건 스킵 분기를 타지 않는다 — 그 스킵이
     * "인증 실패로 0건"을 "정상 0건"과 묶어 삼켜서 2026-08-03 키 만료가 9일간 방치됐다.
     *
     * <p>같은 날 4회 수집이 모두 실패해도 멱등키(BIZ-012)가 첫 1통만 통과시킨다.
     */
    @Async
    @EventListener
    public void onCollectionFailed(CollectionFailedEvent event) {
        String reason = event.isAuthFailure()
                ? "SAM.gov API 인증 실패(HTTP " + event.getLastStatus() + ") — API 키가 만료된 것으로 보입니다."
                : "SAM.gov 수집 중 오류가 발생했습니다. (오류 " + event.getErrorCount() + "건)";

        String actionGuide = event.isAuthFailure()
                ? "SAM.gov > Account Details > Public API Keys 에서 키를 재발급받은 뒤, 운영 서버의 "
                  + ".env 파일에서 SAM_GOV_API_KEY 를 교체하고 컨테이너를 재기동하세요. (키는 90일 만료)"
                : "서버 로그의 [SAM-QUOTA] 항목과 sam_api_call_log 테이블을 확인하세요.";

        String subject = event.isAuthFailure()
                ? "[Bidding Agency] ⚠️ 공고 수집 실패 (API 키 확인 필요)"
                : "[Bidding Agency] ⚠️ 공고 수집 오류";

        Map<String, Object> vars = Map.of(
                "keyword", String.valueOf(event.getKeyword()),
                "errorCount", String.valueOf(event.getErrorCount()),
                "lastStatus", event.getLastStatus() == null ? "-" : String.valueOf(event.getLastStatus()),
                "reason", reason,
                "actionGuide", actionGuide);

        // 하루 1통. 시각은 KST 기준(전역 규칙: 타임존 명시) — 관리자가 보는 날짜와 일치시킨다.
        String today = LocalDate.now(KST).format(DATE_FMT);
        String idempotencyKey = "COLLECTION_FAIL_" + event.getKeyword() + "_" + today;

        log.warn("CR-126 수집 실패 알림 발송: authFailure={} lastStatus={} errors={}",
                event.isAuthFailure(), event.getLastStatus(), event.getErrorCount());

        for (Member admin : memberRepository.findByRole(Member.Role.ADMIN)) {
            notificationService.sendNotification(
                    NotificationType.COLLECTION_FAILED,
                    admin.getEmail(), admin.getId(),
                    subject, vars,
                    null, "CollectorRun",
                    idempotencyKey + "_" + admin.getId());
        }
    }

    @Async
    @EventListener
    public void onBidRequestCreated(BidRequestCreatedEvent event) {
        BidRequest br = bidRequestRepository.findByIdWithDetails(event.getEntityId()).orElse(null);
        String customerName = br != null ? displayName(br.getMember()) : "고객";
        String oppTitle = br != null ? br.getOpportunity().getTitle() : "(공고)";
        String serviceLevel = br != null && br.getServiceLevel() != null
                ? br.getServiceLevel().name() : "-";

        Map<String, Object> vars = Map.of(
                "customerName", customerName,
                "opportunityTitle", oppTitle,
                "serviceLevel", serviceLevel);
        for (Member admin : memberRepository.findByRole(Member.Role.ADMIN)) {
            notificationService.sendNotification(
                    NotificationType.BID_REQUEST_CREATED,
                    admin.getEmail(), admin.getId(),
                    "[Bidding Agency] 새 입찰 신청", vars,
                    event.getEntityId(), "BidRequest",
                    "BID_CREATED_" + event.getEntityId());
        }
    }

    /**
     * CR-018: BidRequest 상태 전이 → 고객 대상 진행 알림 3종.
     * - REVIEW   : 제안서 생성 완료 (GENERATING→REVIEW = AI 생성 완료)
     * - SUBMITTED: 입찰 접수(제출) 완료
     * - AWARDED  : 입찰 낙찰
     * 그 외 전이(NOT_AWARDED 포함)는 고객 알림 미발송(MVP).
     */
    @Async
    @EventListener
    public void onBidRequestStateChanged(BidRequestStateChangedEvent event) {
        BidRequestState toState = event.getToState();
        NotificationType type;
        String subject;
        switch (toState) {
            case REVIEW -> {
                type = NotificationType.PROPOSAL_READY_CUSTOMER;
                subject = "제안서가 준비되었습니다";
            }
            case SUBMITTED -> {
                type = NotificationType.BID_SUBMITTED_CUSTOMER;
                subject = "입찰 제출이 접수되었습니다";
            }
            case AWARDED -> {
                type = NotificationType.BID_AWARDED_CUSTOMER;
                subject = "입찰 결과: 낙찰";
            }
            default -> { return; }  // 그 외 상태는 고객 알림 없음
        }

        BidRequest br = bidRequestRepository.findByIdWithDetails(event.getEntityId()).orElse(null);
        if (br == null) {
            log.warn("BidRequestStateChanged: bid request not found, id={}", event.getEntityId());
            return;
        }
        Member customer = br.getMember();
        String customerName = displayName(customer);
        String oppTitle = br.getOpportunity() != null ? br.getOpportunity().getTitle() : "(공고)";
        String noticeId = br.getOpportunity() != null && br.getOpportunity().getNoticeId() != null
                ? br.getOpportunity().getNoticeId() : "-";
        String deadline = br.getOpportunity() != null && br.getOpportunity().getResponseDeadline() != null
                ? br.getOpportunity().getResponseDeadline().format(DATE_FMT) : "미정";
        String serviceLevel = br.getServiceLevel() != null ? br.getServiceLevel().name() : "-";
        String detailUrl = customerPortalUrl + "/proposals/" + br.getId();

        // 재사용 템플릿이 요구하는 변수를 모두 채운다(과잉이어도 무해).
        Map<String, Object> vars = Map.of(
                "customerName", customerName,
                "opportunityTitle", oppTitle,
                "noticeId", noticeId,
                "deadline", deadline,
                "serviceLevel", serviceLevel,
                "detailUrl", detailUrl);

        notificationService.sendNotification(
                type, customer.getEmail(), customer.getId(),
                subject, vars,
                br.getId(), "BidRequest",
                "BR_STATE_" + br.getId() + "_" + toState);
        log.info("CR-018 고객 알림 발송: bidRequest={}, toState={}, 수신={}", br.getId(), toState, customer.getEmail());
    }

    @Async
    @EventListener
    public void onDocumentGenerated(DocumentGeneratedEvent event) {
        BidRequest br = bidRequestRepository.findByIdWithDetails(event.getBidRequestId()).orElse(null);
        String customerName = br != null ? displayName(br.getMember()) : "고객";
        String oppTitle = br != null ? br.getOpportunity().getTitle() : "(공고)";

        Map<String, Object> vars = Map.of(
                "customerName", customerName,
                "opportunityTitle", oppTitle);
        for (Member admin : memberRepository.findByRole(Member.Role.ADMIN)) {
            notificationService.sendNotification(
                    NotificationType.DOCUMENT_GENERATED,
                    admin.getEmail(), admin.getId(),
                    "[Bidding Agency] 문서 생성 완료 - " + event.getDocumentType(), vars,
                    event.getBidRequestId(), "BidDocument",
                    "DOC_GEN_" + event.getEntityId() + "_v" + event.getVersionNo());
        }
    }

    @Async
    @EventListener
    public void onOpportunityAnalysisCompleted(OpportunityAnalysisCompletedEvent event) {
        Opportunity opp = opportunityRepository.findById(event.getOpportunityId()).orElse(null);
        String oppTitle = opp != null ? opp.getTitle() : "(공고)";

        Map<String, Object> vars = Map.of("opportunityTitle", oppTitle);
        String subject = event.isSuccess()
                ? "[Bidding Agency] 공고 사전 분석 완료"
                : "[Bidding Agency] 공고 사전 분석 실패";
        for (Member admin : memberRepository.findByRole(Member.Role.ADMIN)) {
            notificationService.sendNotification(
                    NotificationType.OPPORTUNITY_ANALYSIS_COMPLETED,
                    admin.getEmail(), admin.getId(),
                    subject, vars,
                    event.getOpportunityId(), "Opportunity",
                    "OPP_ANALYSIS_" + event.getEntityId());
        }
    }

    /**
     * 마감 알림을 관리자 전원과 그 요청을 낸 고객에게 보낸다(BID-NOTIFY-002, CR-156).
     * 멱등 키에 마감일과 받는 사람을 넣어, 받는 사람마다 한 번 보내고 마감이 바뀌면 새 일정으로 다시 보낸다(BIZ-012).
     */
    @Async
    @EventListener
    public void onDeadlineApproaching(DeadlineApproachingEvent event) {
        BidRequest br = bidRequestRepository.findByIdWithDetails(event.getEntityId()).orElse(null);
        if (br == null) {
            log.warn("마감 알림 받을 입찰 요청이 없습니다: bidRequest={}", event.getEntityId());
            return;
        }
        Map<String, Object> vars = Map.of(
                "daysLeft", String.valueOf(event.getDaysRemaining()),
                "opportunityTitle", br.getOpportunity().getTitle());
        String subject = String.format("입찰 마감 D-%d 알림", event.getDaysRemaining());
        String keyPrefix = "DEADLINE_" + event.getEntityId() + "_" + event.getDeadline().toLocalDate()
                + "_" + event.getDaysRemaining() + "_";

        Map<UUID, Member> recipients = new LinkedHashMap<>();
        for (Member admin : memberRepository.findByRole(Member.Role.ADMIN)) recipients.putIfAbsent(admin.getId(), admin);
        recipients.putIfAbsent(br.getMember().getId(), br.getMember());

        for (Member m : recipients.values()) {
            if (m.getEmail() == null || m.getEmail().isBlank()) continue;
            try {
                notificationService.sendNotification(
                        NotificationType.DEADLINE_ALERT, m.getEmail(), m.getId(), subject, vars,
                        event.getEntityId(), "BidRequest", keyPrefix + m.getId());
            } catch (Exception e) {
                log.error("마감 알림 실패: bidRequest={}, member={}", event.getEntityId(), m.getId(), e);
            }
        }
    }

    /**
     * CR-006: 공고 노출 승인(VISIBLE) 시 신규 공고 등록 알림.
     * CR-161: 무료회원은 공고문을 볼 수 없으므로(BIZ-071) 오늘 회원 기간 안의 고객에게만 보낸다.
     */
    @Async
    @EventListener
    public void onOpportunityApproved(OpportunityApprovedEvent event) {
        Opportunity opp = opportunityRepository.findById(event.getEntityId()).orElse(null);
        if (opp == null) {
            log.warn("OpportunityApproved: opportunity not found, id={}", event.getEntityId());
            return;
        }
        String oppTitle = opp.getTitle();
        String noticeId = opp.getNoticeId() != null ? opp.getNoticeId() : "-";
        String deadline = opp.getResponseDeadline() != null
                ? opp.getResponseDeadline().format(DATE_FMT) : "미정";
        // CR-158: /search/{id} 는 공고문 번호를 받는다. 모은 공고 번호로는 원본 공고 상세를 연다(회원은 거기서 공고문으로 간다)
        String detailUrl = customerPortalUrl + "/search/origin/" + opp.getId();

        List<Member> customers = memberRepository.findByRole(Member.Role.CUSTOMER);
        Map<UUID, LocalDate> paid = membershipService.currentEndsOn(
                customers.stream().map(Member::getId).toList());
        List<Member> members = customers.stream().filter(c -> paid.containsKey(c.getId())).toList();
        for (Member customer : members) {
            Map<String, Object> vars = Map.of(
                    "customerName", displayName(customer),
                    "opportunityTitle", oppTitle,
                    "noticeId", noticeId,
                    "deadline", deadline,
                    "detailUrl", detailUrl);
            notificationService.sendNotification(
                    NotificationType.OPPORTUNITY_APPROVED,
                    customer.getEmail(), customer.getId(),
                    "신규 입찰 공고 등록", vars,
                    opp.getId(), "Opportunity",
                    "OPP_APPROVED_" + opp.getId() + "_" + customer.getId());
        }
        log.info("OpportunityApproved 고객 알림 발송: opp={}, 대상 {}명 (고객 {}명 중 회원)",
                opp.getId(), members.size(), customers.size());
    }
}
