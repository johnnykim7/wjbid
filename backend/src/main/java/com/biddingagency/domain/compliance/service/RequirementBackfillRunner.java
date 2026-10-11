package com.biddingagency.domain.compliance.service;

import com.biddingagency.domain.notice.entity.Notice;
import com.biddingagency.domain.notice.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 서버가 뜰 때 분석은 끝났는데 요구 항목이 0행인 공고를 한 번 채운다 (CR-141, BIZ-045).
 * CR-141 전에는 요구 항목을 AI 추출로 채웠고 실제로는 비어 있었으므로, 배포 직후 이미 분석된 공고를 메운다.
 * 이미 항목이 있는 공고는 건너뛴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RequirementBackfillRunner {

    private final NoticeRepository noticeRepository;
    private final RequirementSyncService requirementSyncService;

    @EventListener(ApplicationReadyEvent.class)
    public void backfill() {
        Set<UUID> done = new HashSet<>();
        int failed = 0;
        for (Notice notice : noticeRepository.findCompletedWithoutRequirementItems()) {
            if (!done.add(notice.getOpportunity().getId())) {
                continue;
            }
            try {
                requirementSyncService.syncFromNotice(notice);
            } catch (RuntimeException e) {
                failed++;
                log.error("[CR-141] 요구 항목 채우기 실패: notice={}", notice.getId(), e);
            }
        }
        log.info("[CR-141] 요구 항목 채우기 끝: opportunities={}, failed={}", done.size(), failed);
    }
}
