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
 * 슬롯별 패턴 추출 (CR-013). OpportunityAnalysisService 미러.
 * BIZ-016: 동일 슬롯 2건 이상일 때만 추출. BIZ-017: 출처 보호.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatternExtractionService {

    private final SlotDefinitionRepository slotDefinitionRepository;
    private final SlotAssignmentRepository slotAssignmentRepository;
    private final PatternGuideRepository patternGuideRepository;
    private final LLMPlatformClient llmPlatformClient;

    public List<PatternGuideDto> listGuides() {
        // 슬롯 정렬 순서대로, 각 슬롯의 가이드(있으면) 반환
        List<PatternGuideDto> result = new ArrayList<>();
        for (SlotDefinition slot : slotDefinitionRepository.findAllByOrderByDisplayOrderAsc()) {
            patternGuideRepository.findBySlotDefinitionId(slot.getId())
                    .forEach(g -> result.add(PatternGuideDto.from(g)));
        }
        return result;
    }

    public PatternGuideDto getGuide(String slotCode) {
        SlotDefinition slot = findSlot(slotCode);
        return patternGuideRepository.findBySlotDefinitionIdAndIndustryTypeIsNull(slot.getId())
                .map(PatternGuideDto::from)
                .orElse(null);
    }

    /** BIZ-016: 동일 슬롯 2건 이상일 때만 추출 트리거 */
    @Transactional
    public PatternGuideDto triggerExtraction(String slotCode, IndustryType industryType) {
        SlotDefinition slot = findSlot(slotCode);

        long count = slotAssignmentRepository.findBySlotDefinitionId(slot.getId()).size();
        if (count < 2) {
            throw new IllegalArgumentException(
                    "패턴 추출은 동일 슬롯에 2건 이상 배치됐을 때만 가능합니다 (현재 " + count + "건, BIZ-016)");
        }

        PatternGuide guide = findOrCreateGuide(slot, industryType);
        guide.markExtracting(null);
        patternGuideRepository.save(guide);

        log.info("[패턴] 슬롯 추출 트리거: slot={}, industryType={}, count={}", slotCode, industryType, count);
        extractAsync(slot.getId(), guide.getId(), slotCode, industryType);
        return PatternGuideDto.from(guide);
    }

    @Async("llmTaskExecutor")
    public void extractAsync(UUID slotId, UUID guideId, String slotCode, IndustryType industryType) {
        log.info("[패턴] 슬롯 추출 시작: slot={}", slotCode);
        try {
            SlotDefinition slot = slotDefinitionRepository.findById(slotId)
                    .orElseThrow(() -> new IllegalArgumentException("슬롯을 찾을 수 없습니다: " + slotId));

            // 슬롯에 모인 각 건의 파일 참조(Aimbase가 MCP get_slot_samples로 원본을 가져가 직접 파싱)
            List<Map<String, Object>> samples = new ArrayList<>();
            for (SlotAssignment a : slotAssignmentRepository.findBySlotDefinitionId(slotId)) {
                Map<String, Object> s = new HashMap<>();
                s.put("rfpSampleId", a.getRfpSample().getId().toString());
                s.put("opportunityNo", a.getRfpSample().getOpportunityNo());
                s.put("outcome", a.getRfpSample().getOutcome().name());
                if (a.getSampleFile() != null) {
                    s.put("fileId", a.getSampleFile().getId().toString());
                    s.put("fileName", a.getSampleFile().getFileName());
                }
                if (a.getSectionText() != null) {
                    s.put("sectionText", a.getSectionText());
                }
                samples.add(s);
            }

            Map<String, Object> input = new HashMap<>();
            input.put("slotCode", slotCode);
            input.put("slotLabel", slot.getLabelKo());
            input.put("industryType", industryType != null ? industryType.name() : null);
            input.put("samples", samples);
            input.put("guideId", guideId.toString());

            llmPlatformClient.extractSlotPattern(input);

            // Aimbase 워크플로우가 MCP save_pattern_guide를 콜백하여 결과 저장
            log.info("[패턴] 슬롯 추출 완료 (Aimbase MCP 콜백으로 저장됨): slot={}", slotCode);

        } catch (LLMPlatformException e) {
            log.error("[패턴] Aimbase 오류 (슬롯 추출): slot={}", slotCode, e);
            markFailed(guideId, e.getMessage());
        } catch (Exception e) {
            log.error("[패턴] 예상치 못한 오류 (슬롯 추출): slot={}", slotCode, e);
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
    public void savePatternGuide(String slotCode, IndustryType industryType,
                                 Map<String, Object> guideJson, Integer sampleCount) {
        SlotDefinition slot = findSlot(slotCode);
        PatternGuide guide = findOrCreateGuide(slot, industryType);

        if (!guide.canAutoUpdate()) {
            log.warn("[패턴] 출처 보호로 자동저장 스킵 (source={}): slot={}", guide.getSource(), slotCode);
            return;
        }
        guide.markCompleted(guideJson, sampleCount);
        patternGuideRepository.save(guide);
        log.info("[패턴] 가이드 저장 완료: slot={}, sampleCount={}", slotCode, sampleCount);
    }

    /** 관리자 수동 편집 → source=HUMAN_EDITED */
    @Transactional
    public PatternGuideDto updateGuideManually(String slotCode, PatternGuideEditRequest req) {
        SlotDefinition slot = findSlot(slotCode);
        PatternGuide guide = patternGuideRepository.findBySlotDefinitionIdAndIndustryTypeIsNull(slot.getId())
                .orElseGet(() -> patternGuideRepository.save(PatternGuide.builder()
                        .slotDefinition(slot)
                        .source(GuideSource.HUMAN_ADDED)
                        .build()));
        guide.applyManualEdit(req.guideJson(), req.guideMarkdown());
        patternGuideRepository.save(guide);
        log.info("[패턴] 가이드 수동 편집: slot={}, source={}", slotCode, guide.getSource());
        return PatternGuideDto.from(guide);
    }

    private PatternGuide findOrCreateGuide(SlotDefinition slot, IndustryType industryType) {
        Optional<PatternGuide> existing = (industryType == null)
                ? patternGuideRepository.findBySlotDefinitionIdAndIndustryTypeIsNull(slot.getId())
                : patternGuideRepository.findBySlotDefinitionIdAndIndustryType(slot.getId(), industryType);
        return existing.orElseGet(() -> patternGuideRepository.save(PatternGuide.builder()
                .slotDefinition(slot)
                .industryType(industryType)
                .build()));
    }

    private SlotDefinition findSlot(String slotCode) {
        return slotDefinitionRepository.findBySlotCode(slotCode)
                .orElseThrow(() -> new IllegalArgumentException("슬롯을 찾을 수 없습니다: " + slotCode));
    }
}
