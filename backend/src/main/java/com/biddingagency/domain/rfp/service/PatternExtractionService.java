package com.biddingagency.domain.rfp.service;

import com.biddingagency.domain.rfp.dto.PatternGuideDto;
import com.biddingagency.domain.rfp.dto.PatternGuideEditRequest;
import com.biddingagency.domain.rfp.entity.*;
import com.biddingagency.domain.rfp.repository.*;
import com.biddingagency.integration.llmplatform.LLMPlatformClient;
import com.biddingagency.integration.llmplatform.LLMPlatformException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 공고유형별 패턴 가이드 추출 (CR-013 재설계).
 * 추출 단위 = 공고유형. 그 유형의 성공 제안서들에서 "이 유형 잘 쓰는 법" 가이드를 추출/누적.
 * BIZ-017: 출처 보호 — HUMAN_* 가이드는 자동추출이 덮어쓰지 않음.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatternExtractionService {

    private final RfpSampleRepository rfpSampleRepository;
    private final PatternGuideRepository patternGuideRepository;
    private final LLMPlatformClient llmPlatformClient;

    public List<PatternGuideDto> listGuides() {
        return patternGuideRepository.findAllByOrderByIndustryTypeAsc().stream()
                .map(PatternGuideDto::from)
                .toList();
    }

    public PatternGuideDto getGuide(IndustryType industryType) {
        return patternGuideRepository.findByIndustryType(industryType)
                .map(PatternGuideDto::from)
                .orElse(null);
    }

    /**
     * 공고유형 단위 패턴 추출 트리거. 그 유형의 성공 제안서들을 비교 대상으로 추출/갱신.
     * 1건째도 허용 — 그 1건으로 가이드 v1, 이후 새 건 등록 시 누적 갱신.
     */
    @Transactional
    public PatternGuideDto triggerExtraction(IndustryType industryType) {
        long count = rfpSampleRepository.findByIndustryTypeAndUseForPatternTrue(industryType).size();
        if (count < 1) {
            throw new IllegalArgumentException(
                    "패턴 추출은 해당 유형에 성공 제안서가 1건 이상 등록됐을 때만 가능합니다 (현재 " + count + "건)");
        }

        PatternGuide guide = findOrCreateGuide(industryType);
        guide.markExtracting(null);
        patternGuideRepository.save(guide);

        log.info("[패턴] 유형 추출 트리거: industryType={}, count={}", industryType, count);
        extractAsync(guide.getId(), industryType);
        return PatternGuideDto.from(guide);
    }

    @Async("llmTaskExecutor")
    public void extractAsync(UUID guideId, IndustryType industryType) {
        log.info("[패턴] 유형 추출 시작: industryType={}", industryType);
        try {
            Map<String, Object> input = new HashMap<>();
            input.put("industryType", industryType.name());
            input.put("guideId", guideId.toString());

            // 그 유형의 성공 제안서 목록 (Aimbase가 MCP get_reference_samples로 원본을 가져가 직접 파싱)
            patternGuideRepository.findByIndustryType(industryType).ifPresent(g -> {
                if (g.getGuideJson() != null) input.put("existingGuide", g.getGuideJson());
            });

            llmPlatformClient.extractTypePattern(input);

            // Aimbase 워크플로우가 MCP save_pattern_guide를 콜백하여 결과 저장
            log.info("[패턴] 유형 추출 완료 (Aimbase MCP 콜백으로 저장됨): industryType={}", industryType);

        } catch (LLMPlatformException e) {
            log.error("[패턴] Aimbase 오류 (유형 추출): industryType={}", industryType, e);
            markFailed(guideId, e.getMessage());
        } catch (Exception e) {
            log.error("[패턴] 예상치 못한 오류 (유형 추출): industryType={}", industryType, e);
            markFailed(guideId, e.getMessage());
        }
    }

    @Transactional
    public void markFailed(UUID guideId, String errorMessage) {
        patternGuideRepository.findById(guideId).ifPresent(g -> {
            g.markFailed(errorMessage);
            patternGuideRepository.save(g);
        });
    }

    /** MCP 콜백: Aimbase가 추출 결과 저장. BIZ-017: 사람 편집분은 덮어쓰지 않음. */
    @Transactional
    public void savePatternGuide(IndustryType industryType, Map<String, Object> guideJson, Integer sampleCount) {
        PatternGuide guide = findOrCreateGuide(industryType);

        if (!guide.canAutoUpdate()) {
            log.warn("[패턴] 출처 보호로 자동저장 스킵 (source={}): industryType={}", guide.getSource(), industryType);
            return;
        }
        guide.markCompleted(guideJson, sampleCount);
        patternGuideRepository.save(guide);
        log.info("[패턴] 가이드 저장 완료: industryType={}, sampleCount={}", industryType, sampleCount);
    }

    /** 관리자 수동 편집 → source=HUMAN_EDITED */
    @Transactional
    public PatternGuideDto updateGuideManually(IndustryType industryType, PatternGuideEditRequest req) {
        PatternGuide guide = patternGuideRepository.findByIndustryType(industryType)
                .orElseGet(() -> patternGuideRepository.save(PatternGuide.builder()
                        .industryType(industryType)
                        .source(GuideSource.HUMAN_ADDED)
                        .build()));
        guide.applyManualEdit(req.guideJson(), req.guideMarkdown());
        patternGuideRepository.save(guide);
        log.info("[패턴] 가이드 수동 편집: industryType={}, source={}", industryType, guide.getSource());
        return PatternGuideDto.from(guide);
    }

    private PatternGuide findOrCreateGuide(IndustryType industryType) {
        return patternGuideRepository.findByIndustryType(industryType)
                .orElseGet(() -> patternGuideRepository.save(PatternGuide.builder()
                        .industryType(industryType)
                        .build()));
    }
}
