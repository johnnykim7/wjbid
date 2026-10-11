package com.biddingagency.integration.scheduler;

import com.biddingagency.domain.member.service.DocumentExpiryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 서류 만료 메일 확인 (CR-136, BID-MKT-014). 확인 시각은 설정 app.doc-expiry.cron(한국 시각).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentExpiryScheduler {

    private final DocumentExpiryService documentExpiryService;

    @Scheduled(cron = "${app.doc-expiry.cron:0 0 9 * * *}", zone = DocumentExpiryService.SERVICE_ZONE)
    public void checkExpiries() {
        try {
            documentExpiryService.publishDueAlerts();
        } catch (Exception e) {
            log.error("서류 만료 알림 확인 실패", e);
        }
    }
}
