package com.biddingagency.mcp;

import com.biddingagency.domain.rfp.entity.RfpSampleFile;
import com.biddingagency.domain.rfp.repository.RfpSampleFileRepository;
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

import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.util.UUID;

/**
 * 성공 제안서 원본 파일 다운로드 (CR-013).
 * /mcp/** 경로로 두어 Aimbase(server-to-server, JWT 불필요)가 패턴 추출 시 원본을 가져가 직접 파싱.
 */
@Slf4j
@RestController
@RequestMapping("/mcp/rfp-files")
@RequiredArgsConstructor
public class RfpFileDownloadController {

    private final RfpSampleFileRepository fileRepository;
    private final StorageService storageService;

    @GetMapping("/{fileId}/download")
    public ResponseEntity<Resource> download(@PathVariable UUID fileId) {
        RfpSampleFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("파일을 찾을 수 없습니다: " + fileId));
        byte[] bytes = storageService.load(file.getStorageUrl());
        String contentType = file.getContentType() != null ? file.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        String fileName = file.getFileName() != null ? file.getFileName() : "file";
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");

        log.info("[RFP] 파일 다운로드 (MCP): fileId={}, name={}", fileId, fileName);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .body(new ByteArrayResource(bytes));
    }
}
