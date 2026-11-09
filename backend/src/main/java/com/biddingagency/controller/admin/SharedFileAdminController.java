package com.biddingagency.controller.admin;

import com.biddingagency.domain.sharedfile.service.SharedFileService;
import com.biddingagency.dto.SharedFileResponses;
import com.biddingagency.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/admin/shared-files")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SharedFileAdminController {

    private static final int MAX_PAGE_SIZE = 100;

    private final SharedFileService sharedFileService;

    public record DownloadRequest(@NotBlank String password) {
    }

    @GetMapping
    public ResponseEntity<SharedFileResponses.ListView> list(@RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "20") int size,
                                                             @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(sharedFileService.list(page, clampSize(size), user.getMember()));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SharedFileResponses.Uploaded> upload(@RequestPart("file") MultipartFile file,
                                                               @AuthenticationPrincipal CustomUserDetails user,
                                                               HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sharedFileService.upload(file, user.getMember(), clientIp(request)));
    }

    // StreamingResponseBody 는 비동기 재진입 때 JWT 필터를 다시 타지 않아 인증이 풀리므로 응답에 직접 쓴다
    @PostMapping("/{id}/download")
    public void download(@PathVariable UUID id,
                         @Valid @RequestBody DownloadRequest body,
                         @AuthenticationPrincipal CustomUserDetails user,
                         HttpServletRequest request,
                         HttpServletResponse response) throws IOException {
        SharedFileService.Download download =
                sharedFileService.authorizeDownload(id, body.password(), user.getMember(), clientIp(request));
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setContentLengthLong(download.sizeBytes());
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(download.fileName(), StandardCharsets.UTF_8).build().toString());
        try {
            sharedFileService.writeDecrypted(download, response.getOutputStream());
        } catch (RuntimeException | IOException e) {
            if (!response.isCommitted()) {
                response.reset();
            }
            throw e;
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id,
                                       @AuthenticationPrincipal CustomUserDetails user,
                                       HttpServletRequest request) {
        sharedFileService.delete(id, user.getMember(), clientIp(request));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/logs")
    public ResponseEntity<SharedFileResponses.LogPage> logs(@RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(sharedFileService.logs(page, clampSize(size)));
    }

    private static int clampSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }

    // 상용은 nginx 뒤에 있고 앱 포트가 밖으로 열려 있지 않아 nginx 가 넣은 X-Real-IP 를 믿을 수 있다
    private static String clientIp(HttpServletRequest request) {
        String realIp = request.getHeader("X-Real-IP");
        return realIp == null || realIp.isBlank() ? request.getRemoteAddr() : realIp;
    }
}
