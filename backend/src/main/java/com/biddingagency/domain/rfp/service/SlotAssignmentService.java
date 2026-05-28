package com.biddingagency.domain.rfp.service;

import com.biddingagency.domain.rfp.dto.SlotAssignRequest;
import com.biddingagency.domain.rfp.dto.SlotAssignmentDto;
import com.biddingagency.domain.rfp.entity.*;
import com.biddingagency.domain.rfp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** 슬롯 배치 생성/확인/해제 (CR-013) */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SlotAssignmentService {

    private final RfpSampleRepository rfpSampleRepository;
    private final RfpSampleFileRepository fileRepository;
    private final SlotDefinitionRepository slotDefinitionRepository;
    private final SlotAssignmentRepository slotAssignmentRepository;

    @Transactional
    public SlotAssignmentDto assign(UUID sampleId, String slotCode, SlotAssignRequest req) {
        RfpSample sample = rfpSampleRepository.findById(sampleId)
                .orElseThrow(() -> new IllegalArgumentException("성공 제안서를 찾을 수 없습니다: " + sampleId));
        SlotDefinition slot = slotDefinitionRepository.findBySlotCode(slotCode)
                .orElseThrow(() -> new IllegalArgumentException("슬롯을 찾을 수 없습니다: " + slotCode));

        RfpSampleFile file = null;
        if (req.sampleFileId() != null && !req.sampleFileId().isBlank()) {
            file = fileRepository.findById(UUID.fromString(req.sampleFileId()))
                    .orElseThrow(() -> new IllegalArgumentException("파일을 찾을 수 없습니다: " + req.sampleFileId()));
        }

        SlotAssignment assignment = SlotAssignment.builder()
                .rfpSample(sample)
                .slotDefinition(slot)
                .sampleFile(file)
                .sectionText(req.sectionText())
                .otherLabel(req.otherLabel())
                .confirmed(req.confirmed() != null ? req.confirmed() : true)
                .autoEstimated(false)
                .build();
        SlotAssignment saved = slotAssignmentRepository.save(assignment);
        log.info("[RFP] 슬롯 배치: sample={}, slot={}, file={}", sampleId, slotCode, req.sampleFileId());
        return SlotAssignmentDto.from(saved);
    }

    @Transactional
    public void unassign(UUID assignmentId) {
        slotAssignmentRepository.deleteById(assignmentId);
        log.info("[RFP] 슬롯 배치 해제: assignmentId={}", assignmentId);
    }
}
