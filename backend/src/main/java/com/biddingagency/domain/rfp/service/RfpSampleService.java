package com.biddingagency.domain.rfp.service;

import com.biddingagency.domain.rfp.dto.*;
import com.biddingagency.domain.rfp.entity.*;
import com.biddingagency.domain.rfp.repository.*;
import com.biddingagency.integration.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

/** 성공 제안서 등록/파일/상세 (CR-013) */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RfpSampleService {

    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024; // BIZ-011

    private final RfpSampleRepository rfpSampleRepository;
    private final RfpSampleFileRepository fileRepository;
    private final SlotDefinitionRepository slotDefinitionRepository;
    private final SlotAssignmentRepository slotAssignmentRepository;
    private final StorageService storageService;
    private final SlotEstimator slotEstimator;

    public Page<RfpSampleDto> list(Pageable pageable) {
        return rfpSampleRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(s -> {
                    int fileCount = fileRepository.findByRfpSampleId(s.getId()).size();
                    int assignedSlots = (int) slotAssignmentRepository.findByRfpSampleId(s.getId()).stream()
                            .map(a -> a.getSlotDefinition().getId())
                            .distinct().count();
                    return RfpSampleDto.from(s, fileCount, assignedSlots);
                });
    }

    @Transactional
    public RfpSampleDto create(RfpSampleCreateRequest req) {
        RfpSample sample = RfpSample.builder()
                .opportunityNo(req.opportunityNo())
                .industryType(req.industryType())
                .outcome(req.outcome())
                .company(req.company())
                .agency(req.agency())
                .awardAmount(req.awardAmount())
                .fiscalYear(req.fiscalYear())
                .note(req.note())
                .build();
        RfpSample saved = rfpSampleRepository.save(sample);
        log.info("[RFP] 성공 제안서 등록: id={}, opportunityNo={}", saved.getId(), saved.getOpportunityNo());
        return RfpSampleDto.from(saved, 0, 0);
    }

    public RfpSampleDetailDto getDetail(UUID id) {
        RfpSample sample = findSample(id);
        List<RfpSampleFileDto> files = fileRepository.findByRfpSampleId(id).stream()
                .map(RfpSampleFileDto::from).toList();
        List<SlotStatusDto> slots = buildSlotStatus(id);
        return RfpSampleDetailDto.of(sample, files, slots);
    }

    @Transactional
    public RfpSampleFileDto uploadFile(UUID id, MultipartFile file, boolean isPws) {
        RfpSample sample = findSample(id);
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("파일 크기가 50MB를 초과합니다");
        }
        String storageUrl = storageService.store("rfp/" + id, file);
        RfpSampleFile saved = fileRepository.save(RfpSampleFile.builder()
                .rfpSample(sample)
                .fileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .storageUrl(storageUrl)
                .isPws(isPws)
                .build());

        // 슬롯 자동추정 (PWS는 슬롯 배치 대상 아님)
        if (!isPws) {
            String slotCode = slotEstimator.estimate(file.getOriginalFilename());
            if (slotCode != null) {
                slotDefinitionRepository.findBySlotCode(slotCode).ifPresent(slot ->
                        slotAssignmentRepository.save(SlotAssignment.builder()
                                .rfpSample(sample)
                                .slotDefinition(slot)
                                .sampleFile(saved)
                                .autoEstimated(true)
                                .confirmed(false)
                                .build()));
                log.info("[RFP] 슬롯 자동추정: file={}, slot={}", file.getOriginalFilename(), slotCode);
            }
        }
        return RfpSampleFileDto.from(saved);
    }

    @Transactional
    public void delete(UUID id) {
        RfpSample sample = findSample(id);
        // 원본 파일 디스크 정리
        fileRepository.findByRfpSampleId(id).forEach(f -> storageService.delete(f.getStorageUrl()));
        rfpSampleRepository.delete(sample); // FK CASCADE로 파일/배치 row 제거
        log.info("[RFP] 성공 제안서 삭제: id={}", id);
    }

    @Transactional
    public void deleteFile(UUID id, UUID fileId) {
        RfpSampleFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("파일을 찾을 수 없습니다: " + fileId));
        storageService.delete(file.getStorageUrl());
        fileRepository.delete(file);
    }

    /** 7슬롯(+기타) 전체 + 각 슬롯의 배치 현황. 빈 슬롯도 포함(역요구용). */
    public List<SlotStatusDto> buildSlotStatus(UUID sampleId) {
        List<SlotAssignment> assignments = slotAssignmentRepository.findByRfpSampleId(sampleId);
        Map<UUID, List<SlotAssignmentDto>> bySlot = new HashMap<>();
        for (SlotAssignment a : assignments) {
            bySlot.computeIfAbsent(a.getSlotDefinition().getId(), k -> new ArrayList<>())
                    .add(SlotAssignmentDto.from(a));
        }
        return slotDefinitionRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(slot -> SlotStatusDto.of(slot, bySlot.getOrDefault(slot.getId(), List.of())))
                .toList();
    }

    private RfpSample findSample(UUID id) {
        return rfpSampleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("성공 제안서를 찾을 수 없습니다: " + id));
    }
}
