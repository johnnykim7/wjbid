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

/** 성공 제안서 등록/파일/상세 (CR-013 재설계) — 원본 통째 보관, 슬롯 분류 없음 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RfpSampleService {

    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024; // BIZ-011

    private final RfpSampleRepository rfpSampleRepository;
    private final RfpSampleFileRepository fileRepository;
    private final StorageService storageService;

    public Page<RfpSampleDto> list(Pageable pageable) {
        return rfpSampleRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(s -> {
                    int fileCount = fileRepository.findByRfpSampleId(s.getId()).size();
                    return RfpSampleDto.from(s, fileCount);
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
        return RfpSampleDto.from(saved, 0);
    }

    public RfpSampleDetailDto getDetail(UUID id) {
        RfpSample sample = findSample(id);
        List<RfpSampleFileDto> files = fileRepository.findByRfpSampleId(id).stream()
                .map(RfpSampleFileDto::from).toList();
        return RfpSampleDetailDto.of(sample, files);
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
        log.info("[RFP] 파일 업로드: sample={}, file={}", id, file.getOriginalFilename());
        return RfpSampleFileDto.from(saved);
    }

    @Transactional
    public void delete(UUID id) {
        RfpSample sample = findSample(id);
        // 원본 파일 디스크 정리
        fileRepository.findByRfpSampleId(id).forEach(f -> storageService.delete(f.getStorageUrl()));
        rfpSampleRepository.delete(sample); // FK CASCADE로 파일 row 제거
        log.info("[RFP] 성공 제안서 삭제: id={}", id);
    }

    @Transactional
    public void deleteFile(UUID id, UUID fileId) {
        RfpSampleFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("파일을 찾을 수 없습니다: " + fileId));
        storageService.delete(file.getStorageUrl());
        fileRepository.delete(file);
    }

    private RfpSample findSample(UUID id) {
        return rfpSampleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("성공 제안서를 찾을 수 없습니다: " + id));
    }
}
