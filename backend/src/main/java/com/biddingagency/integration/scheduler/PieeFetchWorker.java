package com.biddingagency.integration.scheduler;

import com.biddingagency.domain.opportunity.entity.AttachmentDownloadStatus;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.entity.PieeFetchStatus;
import com.biddingagency.domain.opportunity.repository.OpportunityAttachmentRepository;
import com.biddingagency.domain.opportunity.repository.OpportunityRepository;
import com.biddingagency.domain.opportunity.service.PieeFetchService;
import com.biddingagency.domain.opportunity.service.PieeSettings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * PIEE 입찰서류 받기 일꾼(BIZ-079, CR-160). 수집과 따로 돌아 받기가 실패해도 수집을 막지 않는다.
 *
 * <p>한 번에 한 공고만 받는다. 받기를 걸어 둔 공고 중 오래 기다린 것부터, 그 공고의 SAM 첨부를 다 받았거나
 * SAM 기다림 상한이 지난 것을 잡는다. 받기는 수 분 걸려 스케줄러 스레드를 붙잡지 않도록 따로 돌린다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PieeFetchWorker {

    static final String STALE_MESSAGE = "받는 중 멈춤";

    private final OpportunityRepository opportunityRepository;
    private final OpportunityAttachmentRepository attachmentRepository;
    private final PieeFetchService pieeFetchService;
    private final PieeSettings settings;

    @Scheduled(fixedDelayString = "${app.piee.fetch-interval-ms:60000}",
            initialDelayString = "${app.piee.fetch-interval-ms:60000}")
    public void tick() {
        LocalDateTime now = settings.now();
        // 받는 중에 서버가 내려가면 FETCHING 이 남아 다음 받기가 영영 막힌다
        LocalDateTime staleBefore = now.minus(settings.timeout().multipliedBy(2));
        int stale = opportunityRepository.failStalePieeFetch(PieeFetchStatus.FETCHING, PieeFetchStatus.FAILED,
                staleBefore, now, STALE_MESSAGE);
        if (stale > 0) {
            log.warn("[CR-160] 받는 중 멈춘 공고 {}건을 받기 실패로 바꿈", stale);
        }
        if (opportunityRepository.countByPieeFetchStatus(PieeFetchStatus.FETCHING) > 0) {
            return;
        }
        LocalDateTime samWaitStarted = now.minus(settings.samWait());
        for (Opportunity opp : opportunityRepository.findByPieeFetchStatusOrderByPieeFetchStatusAtAsc(PieeFetchStatus.WAITING)) {
            boolean samBusy = attachmentRepository.countByOpportunityIdAndDownloadStatus(
                    opp.getId(), AttachmentDownloadStatus.PENDING) > 0;
            if (samBusy && opp.getPieeFetchStatusAt() != null && opp.getPieeFetchStatusAt().isAfter(samWaitStarted)) {
                continue;
            }
            if (opportunityRepository.changePieeFetchStatus(opp.getId(), PieeFetchStatus.WAITING,
                    PieeFetchStatus.FETCHING, now) == 0) {
                return;
            }
            log.info("[CR-160] PIEE 받기 시작: noticeId={}, solNo={}, SAM 첨부 받는 중={}",
                    opp.getNoticeId(), opp.getSolicitationNumber(), samBusy);
            pieeFetchService.fetchAsync(opp.getId());
            return;
        }
    }
}
