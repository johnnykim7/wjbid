package com.biddingagency.domain.bid.entity;

import com.biddingagency.common.BaseEntity;
import com.biddingagency.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "client_documents",
        indexes = {
                @Index(name = "idx_client_documents_bid_request", columnList = "bid_request_id"),
                @Index(name = "idx_client_documents_member", columnList = "member_id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ClientDocument extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bid_request_id", nullable = false, columnDefinition = "BINARY(16)")
    private BidRequest bidRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, columnDefinition = "BINARY(16)")
    private Member member;

    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "storage_url", columnDefinition = "TEXT", nullable = false)
    private String storageUrl;

    @Column(name = "document_category", length = 100)
    private String documentCategory;
}
