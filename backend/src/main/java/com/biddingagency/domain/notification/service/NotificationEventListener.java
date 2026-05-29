package com.biddingagency.domain.notification.service;

import com.biddingagency.domain.bid.entity.BidRequest;
import com.biddingagency.domain.bid.repository.BidRequestRepository;
import com.biddingagency.domain.event.BidRequestCreatedEvent;
import com.biddingagency.domain.event.DeadlineApproachingEvent;
import com.biddingagency.domain.event.DocumentGeneratedEvent;
import com.biddingagency.domain.event.OpportunitiesCollectedEvent;
import com.biddingagency.domain.event.OpportunityAnalysisCompletedEvent;
import com.biddingagency.domain.event.OpportunityApprovedEvent;
import com.biddingagency.domain.member.entity.Member;
import com.biddingagency.domain.member.repository.MemberRepository;
import com.biddingagency.domain.notification.entity.NotificationType;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.repository.OpportunityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

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

    private final NotificationService notificationService;
    private final MemberRepository memberRepository;
    private final OpportunityRepository opportunityRepository;
    private final BidRequestRepository bidRequestRepository;

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
                    "COLLECTION_" + event.getEntityId());
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
                    "DOC_GEN_" + event.getEntityId());
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

    @Async
    @EventListener
    public void onDeadlineApproaching(DeadlineApproachingEvent event) {
        NotificationType type = switch (event.getDaysRemaining()) {
            case 7 -> NotificationType.DEADLINE_D7;
            case 3 -> NotificationType.DEADLINE_D3;
            case 1 -> NotificationType.DEADLINE_D1;
            default -> NotificationType.DEADLINE_D7;
        };
        BidRequest br = bidRequestRepository.findByIdWithDetails(event.getEntityId()).orElse(null);
        String oppTitle = br != null ? br.getOpportunity().getTitle() : "(공고)";

        Map<String, Object> vars = Map.of(
                "daysLeft", String.valueOf(event.getDaysRemaining()),
                "opportunityTitle", oppTitle);
        for (Member admin : memberRepository.findByRole(Member.Role.ADMIN)) {
            notificationService.sendNotification(
                    type, admin.getEmail(), admin.getId(),
                    String.format("[Bidding Agency] 마감 D-%d 알림", event.getDaysRemaining()), vars,
                    event.getEntityId(), "BidRequest",
                    "DEADLINE_D" + event.getDaysRemaining() + "_" + event.getEntityId());
        }
    }

    /**
     * CR-006: 공고 노출 승인(VISIBLE) 시 전체 고객에게 신규 공고 등록 알림.
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
        String detailUrl = customerPortalUrl + "/search/" + opp.getId();

        List<Member> customers = memberRepository.findByRole(Member.Role.CUSTOMER);
        for (Member customer : customers) {
            Map<String, Object> vars = Map.of(
                    "customerName", displayName(customer),
                    "opportunityTitle", oppTitle,
                    "noticeId", noticeId,
                    "deadline", deadline,
                    "detailUrl", detailUrl);
            notificationService.sendNotification(
                    NotificationType.OPPORTUNITY_APPROVED,
                    customer.getEmail(), customer.getId(),
                    "[Bidding Agency] 신규 입찰 공고 등록", vars,
                    opp.getId(), "Opportunity",
                    "OPP_APPROVED_" + opp.getId() + "_" + customer.getId());
        }
        log.info("OpportunityApproved 고객 알림 발송: opp={}, 대상 {}명", opp.getId(), customers.size());
    }
}
