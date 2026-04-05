package com.biddingagency.domain.notification.service;

import com.biddingagency.domain.event.BidRequestCreatedEvent;
import com.biddingagency.domain.event.BidRequestStateChangedEvent;
import com.biddingagency.domain.event.DeadlineApproachingEvent;
import com.biddingagency.domain.event.DocumentGeneratedEvent;
import com.biddingagency.domain.event.OpportunitiesCollectedEvent;
import com.biddingagency.domain.event.OpportunityAnalysisCompletedEvent;
import com.biddingagency.domain.member.entity.Member;
import com.biddingagency.domain.member.repository.MemberRepository;
import com.biddingagency.domain.notification.entity.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final MemberRepository memberRepository;

    @Async
    @EventListener
    public void onOpportunitiesCollected(OpportunitiesCollectedEvent event) {
        List<Member> admins = memberRepository.findByRole(Member.Role.ADMIN);
        for (Member admin : admins) {
            notificationService.sendNotification(
                    NotificationType.COLLECTION_COMPLETE,
                    admin.getEmail(), admin.getId(),
                    "[Bidding Agency] 공고 수집 완료",
                    String.format("수집 완료: 신규 %d건, 갱신 %d건, 총 %d건",
                            event.getNewCount(), event.getUpdatedCount(), event.getTotalFetched()),
                    event.getEntityId(), "CollectorRun",
                    "COLLECTION_" + event.getEntityId()
            );
        }
    }

    @Async
    @EventListener
    public void onBidRequestCreated(BidRequestCreatedEvent event) {
        List<Member> admins = memberRepository.findByRole(Member.Role.ADMIN);
        for (Member admin : admins) {
            notificationService.sendNotification(
                    NotificationType.BID_REQUEST_CREATED,
                    admin.getEmail(), admin.getId(),
                    "[Bidding Agency] 새 입찰 신청",
                    "새로운 입찰 신청이 접수되었습니다. ID: " + event.getEntityId(),
                    event.getEntityId(), "BidRequest",
                    "BID_CREATED_" + event.getEntityId()
            );
        }
    }

    @Async
    @EventListener
    public void onDocumentGenerated(DocumentGeneratedEvent event) {
        List<Member> admins = memberRepository.findByRole(Member.Role.ADMIN);
        for (Member admin : admins) {
            notificationService.sendNotification(
                    NotificationType.DOCUMENT_GENERATED,
                    admin.getEmail(), admin.getId(),
                    "[Bidding Agency] 문서 생성 완료 - " + event.getDocumentType(),
                    "AI가 문서를 생성했습니다. 검토해주세요. 문서 ID: " + event.getEntityId(),
                    event.getBidRequestId(), "BidDocument",
                    "DOC_GEN_" + event.getEntityId()
            );
        }
    }

    @Async
    @EventListener
    public void onOpportunityAnalysisCompleted(OpportunityAnalysisCompletedEvent event) {
        List<Member> admins = memberRepository.findByRole(Member.Role.ADMIN);
        String subject = event.isSuccess()
                ? "[Bidding Agency] 공고 사전 분석 완료"
                : "[Bidding Agency] 공고 사전 분석 실패";
        String body = event.isSuccess()
                ? "공고 사전 분석이 완료되었습니다. 검수 후 노출 승인해주세요. 공고 ID: " + event.getOpportunityId()
                : "공고 사전 분석이 실패했습니다. 오류: " + event.getErrorMessage();

        for (Member admin : admins) {
            notificationService.sendNotification(
                    NotificationType.OPPORTUNITY_ANALYSIS_COMPLETED,
                    admin.getEmail(), admin.getId(),
                    subject, body,
                    event.getOpportunityId(), "Opportunity",
                    "OPP_ANALYSIS_" + event.getEntityId()
            );
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

        List<Member> admins = memberRepository.findByRole(Member.Role.ADMIN);
        for (Member admin : admins) {
            notificationService.sendNotification(
                    type, admin.getEmail(), admin.getId(),
                    String.format("[Bidding Agency] 마감 D-%d 알림", event.getDaysRemaining()),
                    String.format("입찰 마감이 %d일 남았습니다. 마감: %s", event.getDaysRemaining(), event.getDeadline()),
                    event.getEntityId(), "BidRequest",
                    "DEADLINE_D" + event.getDaysRemaining() + "_" + event.getEntityId()
            );
        }
    }
}
