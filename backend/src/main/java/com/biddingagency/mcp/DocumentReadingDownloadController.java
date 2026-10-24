package com.biddingagency.mcp;

import com.biddingagency.domain.bid.repository.ClientDocumentRepository;
import com.biddingagency.domain.member.repository.MemberDocumentRepository;
import com.biddingagency.domain.reading.entity.DocumentReading;
import com.biddingagency.domain.reading.entity.ReadAs;
import com.biddingagency.domain.reading.repository.DocumentReadingRepository;
import com.biddingagency.domain.reading.service.ReadableForm;
import com.biddingagency.integration.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

/**
 * 고객 서류를 읽는 꼴로 내려받기 (CR-169, T3-2).
 * 고객 서류 읽기 워크플로우의 download_file 이 부른다. 읽기 줄은 내용 해시로만 서류와 이어지므로
 * 같은 해시의 공고별 서류나 공통 서류함 서류 가운데 하나의 파일을 준다.
 */
@Slf4j
@RestController
@RequestMapping("/mcp/document-readings")
@RequiredArgsConstructor
public class DocumentReadingDownloadController {

    private final DocumentReadingRepository readingRepository;
    private final ClientDocumentRepository clientDocumentRepository;
    private final MemberDocumentRepository memberDocumentRepository;
    private final StorageService storageService;

    @GetMapping("/{readingId}/download")
    public ResponseEntity<Resource> download(@PathVariable UUID readingId) {
        Optional<DocumentReading> found = readingRepository.findById(readingId);
        if (found.isEmpty() || found.get().getReadAs() == ReadAs.NONE) {
            return ResponseEntity.notFound().build();
        }
        DocumentReading reading = found.get();
        Optional<String> storageUrl = clientDocumentRepository.findFirstByContentSha256(reading.getContentSha256())
                .map(d -> d.getStorageUrl())
                .or(() -> memberDocumentRepository.findFirstByContentSha256(reading.getContentSha256())
                        .map(d -> d.getStorageUrl()));
        if (storageUrl.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        byte[] bytes;
        try {
            bytes = storageService.load(storageUrl.get());
        } catch (Exception e) {
            log.warn("[CR-169] 고객 서류 파일 없음: reading={}, reason={}", readingId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
        ReadableForm.Rendered rendered = ReadableForm.render(bytes, reading.getFileName(), reading.getContentType());
        String fileName = readingId + "." + ReadableForm.workspaceExtension(reading.getFileName(), reading.getContentType());
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");

        log.info("[CR-169] 고객 서류 내려받기 (MCP): reading={}, readAs={}", readingId, reading.getReadAs());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(rendered.mediaType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .body(new ByteArrayResource(rendered.content()));
    }
}
