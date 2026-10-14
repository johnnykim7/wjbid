package com.biddingagency.domain.opportunity.service;

import com.biddingagency.domain.opportunity.entity.AttachmentDownloadStatus;
import com.biddingagency.domain.opportunity.entity.Opportunity;
import com.biddingagency.domain.opportunity.entity.OpportunityAttachment;
import com.biddingagency.domain.opportunity.entity.PieeFetchStatus;
import com.biddingagency.domain.opportunity.repository.OpportunityAttachmentRepository;
import com.biddingagency.domain.opportunity.repository.OpportunityRepository;
import com.biddingagency.integration.piee.PieeClient;
import com.biddingagency.integration.piee.PieeFetchException;
import com.biddingagency.integration.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * PIEE 입찰서류 압축을 받아 파일마다 공고 첨부로 넣는다(BIZ-079, BIZ-080, CR-160).
 *
 * <p>압축은 수백 MB 일 수 있어 임시 파일로 받고 파일 하나씩 흘려 저장한다. 받는 데 수 분이 걸리므로
 * 트랜잭션으로 묶지 않고 행마다 저장한다. 중간에 끊겨도 넣은 행은 남고, 다시 받으면 내용 해시로 걸러진다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PieeFetchService {

    static final String SOURCE_PREFIX = "piee:";
    static final int MESSAGE_MAX = 500;

    private final OpportunityRepository opportunityRepository;
    private final OpportunityAttachmentRepository attachmentRepository;
    private final StorageService storageService;
    private final PieeClient pieeClient;
    private final PieeSettings settings;

    /** 받기 일꾼이 받는 중(FETCHING)으로 잡은 공고를 한 줄로 받는다 */
    @Async("pieeFetchExecutor")
    public void fetchAsync(UUID opportunityId) {
        fetch(opportunityId);
    }

    public void fetch(UUID opportunityId) {
        Opportunity opp = opportunityRepository.findById(opportunityId).orElse(null);
        if (opp == null) {
            log.warn("[CR-160] PIEE 받기 대상 공고 없음: {}", opportunityId);
            return;
        }
        Optional<String> url = settings.noticeUrl(opp.getSolicitationNumber());
        if (url.isEmpty()) {
            finish(opportunityId, PieeFetchStatus.FAILED, null, "PIEE 공고 주소 설정이 비어 있거나 공고번호가 없음");
            return;
        }
        Path zip = null;
        try {
            zip = Files.createTempFile("piee-", ".zip");
            PieeClient.Outcome outcome = pieeClient.download(url.get(), zip, settings.maxZipBytes(), settings.timeout());
            switch (outcome) {
                case NOT_FOUND -> finish(opportunityId, PieeFetchStatus.NOT_FOUND, null, null);
                case NO_ATTACHMENTS -> finish(opportunityId, PieeFetchStatus.DONE, 0, null);
                case DOWNLOADED -> finish(opportunityId, PieeFetchStatus.DONE, ingestZip(opp, zip), null);
            }
        } catch (PieeFetchException e) {
            log.warn("[CR-160] PIEE 받기 실패: noticeId={}, solNo={}, {}", opp.getNoticeId(),
                    opp.getSolicitationNumber(), e.getMessage());
            finish(opportunityId, PieeFetchStatus.FAILED, null, e.getMessage());
        } catch (IOException | RuntimeException e) {
            log.error("[CR-160] PIEE 받기 오류: noticeId={}, solNo={}", opp.getNoticeId(), opp.getSolicitationNumber(), e);
            finish(opportunityId, PieeFetchStatus.FAILED, null, e.getClass().getSimpleName() + ": " + e.getMessage());
        } finally {
            deleteQuietly(zip);
        }
    }

    /** @return 압축에 든 파일 수 */
    int ingestZip(Opportunity opp, Path zip) throws IOException {
        List<OpportunityAttachment> existing = attachmentRepository.findByOpportunityId(opp.getId());
        Set<String> storedHashes = backfillHashes(existing);
        List<OpportunityAttachment> manualRows = new ArrayList<>(existing.stream()
                .filter(a -> a.getDownloadStatus() == AttachmentDownloadStatus.MANUAL_FETCH_REQUIRED)
                .toList());
        String solNo = opp.getSolicitationNumber().trim();
        int fileCount = 0;
        int added = 0;
        try (ZipFile zipFile = new ZipFile(zip.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String fileName = entry.isDirectory() ? null : AttachmentFiles.flatten(entry.getName());
                if (fileName == null || fileName.isBlank()) continue;
                fileCount++;
                AttachmentFiles.Digest digest;
                try (InputStream in = zipFile.getInputStream(entry)) {
                    digest = AttachmentFiles.sha256(in);
                }
                if (!storedHashes.add(digest.sha256())) {
                    log.info("[CR-160] PIEE 파일 내용이 이미 있어 넣지 않음: noticeId={}, file={}", opp.getNoticeId(), fileName);
                    continue;
                }
                String storageUrl = sharedStorage(digest.sha256(), solNo, opp.getId());
                if (storageUrl == null) {
                    try (InputStream in = zipFile.getInputStream(entry)) {
                        storageUrl = storageService.store(AttachmentFiles.storagePrefix(opp.getId()), fileName, in);
                    }
                }
                OpportunityAttachment row = takeManualRow(manualRows, fileName)
                        .orElseGet(() -> OpportunityAttachment.builder()
                                .opportunity(opp)
                                .sourceUrl(SOURCE_PREFIX + solNo + "/" + entry.getName())
                                .build());
                row.applyUpload(fileName, digest.size(), AttachmentFiles.guessContentType(fileName), storageUrl);
                row.recordContentHash(digest.sha256());
                attachmentRepository.save(row);
                added++;
            }
        }
        log.info("[CR-160] PIEE 입찰서류 받음: noticeId={}, solNo={}, 압축 안 파일={}, 새로 넣음={}",
                opp.getNoticeId(), solNo, fileCount, added);
        return fileCount;
    }

    /** 받은 첨부의 해시를 모은다. CR-160 전에 저장돼 해시가 빈 첨부는 저장 파일을 읽어 채운다 */
    private Set<String> backfillHashes(List<OpportunityAttachment> existing) {
        Set<String> hashes = new HashSet<>();
        for (OpportunityAttachment a : existing) {
            if (a.getDownloadStatus() != AttachmentDownloadStatus.SUCCESS || a.getStorageUrl() == null) continue;
            if (a.getContentSha256() == null) {
                try {
                    a.recordContentHash(AttachmentFiles.sha256(storageService.load(a.getStorageUrl())));
                    attachmentRepository.save(a);
                } catch (RuntimeException e) {
                    log.warn("[CR-160] 옛 첨부 해시를 채우지 못함: attachmentId={}, {}", a.getId(), e.getMessage());
                    continue;
                }
            }
            hashes.add(a.getContentSha256());
        }
        return hashes;
    }

    private String sharedStorage(String sha256, String solNo, UUID opportunityId) {
        return attachmentRepository.findSharable(sha256, solNo, opportunityId,
                        AttachmentDownloadStatus.SUCCESS, AttachmentFiles.ADMIN_UPLOAD_SOURCE)
                .stream()
                .map(OpportunityAttachment::getStorageUrl)
                .findFirst()
                .orElse(null);
    }

    private static Optional<OpportunityAttachment> takeManualRow(List<OpportunityAttachment> manualRows, String fileName) {
        for (int i = 0; i < manualRows.size(); i++) {
            if (fileName.equals(manualRows.get(i).getFileName())) {
                return Optional.of(manualRows.remove(i));
            }
        }
        return Optional.empty();
    }

    private void finish(UUID opportunityId, PieeFetchStatus status, Integer fileCount, String message) {
        String trimmed = message != null && message.length() > MESSAGE_MAX ? message.substring(0, MESSAGE_MAX) : message;
        int updated = opportunityRepository.finishPieeFetch(opportunityId, PieeFetchStatus.FETCHING, status,
                settings.now(), fileCount, trimmed);
        if (updated == 0) {
            log.warn("[CR-160] 받는 중이 아니어서 받기 결과를 남기지 않음: opportunityId={}, 결과={}", opportunityId, status);
        }
    }

    private static void deleteQuietly(Path file) {
        if (file == null) return;
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("[CR-160] 임시 압축을 지우지 못함: {}", file, e);
        }
    }
}
