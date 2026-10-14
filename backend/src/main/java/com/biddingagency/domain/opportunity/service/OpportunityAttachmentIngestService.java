package com.biddingagency.domain.opportunity.service;

import com.biddingagency.domain.opportunity.entity.AttachmentDownloadStatus;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.entity.OpportunityAttachment;
import com.biddingagency.domain.opportunity.repository.OpportunityAttachmentRepository;
import com.biddingagency.domain.opportunity.repository.OpportunityRepository;
import com.biddingagency.integration.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 관리자가 손으로 올린 공고 첨부를 넣는다(CR-019, CR-037). 압축은 풀어 파일마다 넣고,
 * 같은 파일 이름의 가져와야 함 첨부가 있으면 그 행을 채운다. 저장할 때 내용 해시를 남긴다(BIZ-080, CR-160).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpportunityAttachmentIngestService {

    private final OpportunityRepository opportunityRepository;
    private final OpportunityAttachmentRepository attachmentRepository;
    private final StorageService storageService;

    /** @return 넣은 파일 이름들 */
    @Transactional
    public List<String> ingestUploads(UUID opportunityId, List<MultipartFile> files) {
        Opportunity opp = opportunityRepository.findById(opportunityId)
                .orElseThrow(() -> new IllegalArgumentException("Opportunity not found: " + opportunityId));
        List<String> uploaded = new ArrayList<>();
        for (MultipartFile file : files) {
            String originalName = file.getOriginalFilename();
            if (AttachmentFiles.isZip(originalName, file.getContentType())) {
                ingestZip(opp, file, uploaded);
            } else {
                String fileName = AttachmentFiles.flatten(originalName);
                String sha256 = hash(file);
                String storageUrl = storageService.store(AttachmentFiles.storagePrefix(opportunityId), file);
                save(opp, fileName, file.getSize(), file.getContentType(), storageUrl, sha256);
                uploaded.add(fileName != null ? fileName : "");
                log.info("[CR-019] 관리자 첨부 업로드(SUCCESS): opportunityId={}, fileName={}, storageUrl={}",
                        opportunityId, fileName, storageUrl);
            }
        }
        return uploaded;
    }

    private void ingestZip(Opportunity opp, MultipartFile file, List<String> uploaded) {
        String originalName = file.getOriginalFilename();
        try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.isDirectory() ? null : AttachmentFiles.flatten(entry.getName());
                if (entryName == null || entryName.isBlank()) {
                    zis.closeEntry();
                    continue;
                }
                byte[] content = zis.readAllBytes();
                zis.closeEntry();
                if (content.length == 0) continue;
                String storageUrl = storageService.store(AttachmentFiles.storagePrefix(opp.getId()), entryName, content);
                save(opp, entryName, (long) content.length, AttachmentFiles.guessContentType(entryName), storageUrl,
                        AttachmentFiles.sha256(content));
                uploaded.add(entryName);
                log.info("[CR-037] ZIP 엔트리 업로드: opportunityId={}, zip={}, entry={}, size={}",
                        opp.getId(), originalName, entryName, content.length);
            }
        } catch (IOException e) {
            log.error("[CR-037] ZIP 해제 실패: opportunityId={}, zip={}", opp.getId(), originalName, e);
            throw new IllegalArgumentException("ZIP 파일 해제 실패: " + originalName);
        }
    }

    private void save(Opportunity opp, String fileName, Long fileSize, String contentType,
                      String storageUrl, String sha256) {
        OpportunityAttachment attachment = attachmentRepository
                .findByOpportunityIdAndDownloadStatus(opp.getId(), AttachmentDownloadStatus.MANUAL_FETCH_REQUIRED)
                .stream()
                .filter(a -> fileName != null && fileName.equals(a.getFileName()))
                .findFirst()
                .orElseGet(() -> OpportunityAttachment.builder()
                        .opportunity(opp)
                        .sourceUrl(AttachmentFiles.ADMIN_UPLOAD_SOURCE)
                        .build());
        attachment.applyUpload(fileName, fileSize, contentType, storageUrl);
        attachment.recordContentHash(sha256);
        attachmentRepository.save(attachment);
    }

    private String hash(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            return AttachmentFiles.sha256(in).sha256();
        } catch (IOException e) {
            throw new IllegalArgumentException("파일을 읽지 못함: " + file.getOriginalFilename(), e);
        }
    }
}
