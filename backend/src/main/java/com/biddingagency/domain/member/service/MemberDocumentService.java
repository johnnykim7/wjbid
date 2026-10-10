package com.biddingagency.domain.member.service;

import com.biddingagency.domain.member.entity.DocumentCategory;
import com.biddingagency.domain.member.entity.MemberDocument;
import com.biddingagency.domain.member.repository.MemberDocumentRepository;
import com.biddingagency.integration.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;

/**
 * 회사 공통 서류함 서비스 (CR-125)
 *
 * - 고정 카테고리(fixed=true): member당 1건. 재업로드=교체(기존 파일 삭제 후 갱신).
 * - OTHER: 다건 허용. displayName(필수)·description(선택).
 * - 소유 검증: 모든 수정/삭제는 본인 memberId 일치 확인.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberDocumentService {

    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024; // BIZ-011: 50MB

    private final MemberDocumentRepository repository;
    private final StorageService storageService;

    @Transactional(readOnly = true)
    public List<MemberDocument> list(UUID memberId) {
        return repository.findByMemberIdOrderByCreatedAtDesc(memberId);
    }

    @Transactional
    public MemberDocument upload(UUID memberId, DocumentCategory category,
                                 String displayName, String description, MultipartFile file) {
        validateFile(file);

        // 고정 카테고리: 기존 보유분 있으면 교체(파일 삭제 후 레코드 갱신)
        if (category.isFixed()) {
            var existing = repository.findFirstByMemberIdAndCategory(memberId, category);
            if (existing.isPresent()) {
                return replace(memberId, existing.get().getId(),
                        category.getDisplayName(), null, file);
            }
        }

        String storageUrl = storageService.store("member-docs/" + memberId, file);

        MemberDocument doc = MemberDocument.builder()
                .memberId(memberId)
                .category(category)
                .fileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .storageUrl(storageUrl)
                .displayName(resolveDisplayName(category, displayName))
                .description(category.isFixed() ? null : description)
                .build();

        MemberDocument saved = repository.save(doc);
        log.info("[member-doc] uploaded {} ({}) for member {}", saved.getId(), category, memberId);
        return saved;
    }

    @Transactional
    public MemberDocument replace(UUID memberId, UUID docId,
                                  String displayName, String description, MultipartFile file) {
        MemberDocument doc = findOwned(memberId, docId);

        if (file != null && !file.isEmpty()) {
            validateFile(file);
            String oldStorageUrl = doc.getStorageUrl();
            String storageUrl = storageService.store("member-docs/" + memberId, file);
            doc.replaceFile(file.getOriginalFilename(), file.getSize(), file.getContentType(), storageUrl);
            // 기존 파일 삭제(실패해도 레코드 갱신은 진행)
            safeDelete(oldStorageUrl);
        }

        doc.updateMeta(
                resolveDisplayName(doc.getCategory(), displayName),
                doc.getCategory().isFixed() ? null : description);
        log.info("[member-doc] replaced {} for member {}", docId, memberId);
        return doc;
    }

    @Transactional
    public void delete(UUID memberId, UUID docId) {
        MemberDocument doc = findOwned(memberId, docId);
        String storageUrl = doc.getStorageUrl();
        repository.delete(doc);
        safeDelete(storageUrl);
        log.info("[member-doc] deleted {} for member {}", docId, memberId);
    }

    /** CR-125 2단계 자동채움용 — 고정 카테고리별 보유 서류 1건 조회 (없으면 null) */
    @Transactional(readOnly = true)
    public MemberDocument findFixed(UUID memberId, DocumentCategory category) {
        if (!category.isFixed()) {
            return null;
        }
        return repository.findFirstByMemberIdAndCategory(memberId, category).orElse(null);
    }

    // ── helpers ──────────────────────────────────────────────

    private MemberDocument findOwned(UUID memberId, UUID docId) {
        MemberDocument doc = repository.findById(docId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "서류를 찾을 수 없습니다"));
        if (!doc.getMemberId().equals(memberId)) {
            // 소유자 아님 — 존재 노출 방지로 404 (BIZ-125-1)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "서류를 찾을 수 없습니다");
        }
        return doc;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "파일이 비어 있습니다");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "파일 크기가 50MB를 초과합니다");
        }
    }

    private String resolveDisplayName(DocumentCategory category, String displayName) {
        if (category.isFixed()) {
            return category.getDisplayName(); // 고정은 제목 강제
        }
        if (displayName == null || displayName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "문서제목을 입력하세요");
        }
        return displayName.trim();
    }

    private void safeDelete(String storageUrl) {
        try {
            if (storageUrl != null) {
                storageService.delete(storageUrl);
            }
        } catch (Exception e) {
            log.warn("[member-doc] 파일 삭제 실패(무시): {}", storageUrl, e);
        }
    }
}
