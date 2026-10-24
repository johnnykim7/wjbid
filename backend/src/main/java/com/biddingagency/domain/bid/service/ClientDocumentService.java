package com.biddingagency.domain.bid.service;

import com.biddingagency.common.ContentHash;
import com.biddingagency.domain.bid.entity.BidRequest;
import com.biddingagency.domain.bid.entity.ClientDocument;
import com.biddingagency.domain.bid.repository.ClientDocumentRepository;
import com.biddingagency.domain.member.entity.Member;
import com.biddingagency.integration.storage.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * 고객이 입찰 건에 올린 서류. 모두 그 입찰 건을 낸 회원과 관리자만 다룬다 (CR-143, BIZ-060).
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class ClientDocumentService {

    private final ClientDocumentRepository clientDocumentRepository;
    private final BidRequestService bidRequestService;
    private final StorageService storageService;
    private final DataSize maxFileSize;

    public ClientDocumentService(ClientDocumentRepository clientDocumentRepository,
                                 BidRequestService bidRequestService,
                                 StorageService storageService,
                                 @Value("${spring.servlet.multipart.max-file-size}") DataSize maxFileSize) {
        this.clientDocumentRepository = clientDocumentRepository;
        this.bidRequestService = bidRequestService;
        this.storageService = storageService;
        this.maxFileSize = maxFileSize;
    }

    public List<ClientDocument> list(UUID bidRequestId, Member viewer) {
        bidRequestService.findAccessible(bidRequestId, viewer);
        return clientDocumentRepository.findByBidRequestId(bidRequestId);
    }

    /** BIZ-011: 크기 한도는 업로드 설정값을 따른다 */
    @Transactional
    public ClientDocument upload(UUID bidRequestId, Member uploader, MultipartFile file, String category) {
        BidRequest bidRequest = bidRequestService.findAccessible(bidRequestId, uploader);
        if (file.getSize() > maxFileSize.toBytes()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "파일 크기가 " + maxFileSize.toMegabytes() + "MB를 넘습니다");
        }

        // CR-026: AGENT 가 parse_document 로 본문을 가져가야 해서 실파일을 저장한다
        String storageUrl = storageService.store("client-docs/" + bidRequestId, file);
        ClientDocument saved = clientDocumentRepository.save(ClientDocument.builder()
                .bidRequest(bidRequest)
                .member(uploader)
                .fileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .storageUrl(storageUrl)
                .documentCategory(category)
                .contentSha256(ContentHash.of(file))
                .build());
        log.info("Client document uploaded: {} for bid request {}", saved.getId(), bidRequestId);
        return saved;
    }

    /** 경로의 입찰 건 서류만 지운다. 다른 건의 서류 번호를 넣어도 지우지 않는다 */
    @Transactional
    public void delete(UUID bidRequestId, UUID docId, Member viewer) {
        bidRequestService.findAccessible(bidRequestId, viewer);
        ClientDocument doc = clientDocumentRepository.findById(docId)
                .filter(d -> d.getBidRequest().getId().equals(bidRequestId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "서류를 찾을 수 없습니다"));
        clientDocumentRepository.delete(doc);
        log.info("Client document deleted: {} from bid request {}", docId, bidRequestId);
    }
}
