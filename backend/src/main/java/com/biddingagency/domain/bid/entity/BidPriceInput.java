package com.biddingagency.domain.bid.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 가격 표에 고객이 넣은 단가 (CR-194, BIZ-108). 품목과 수량은 공고문 가격 항목을 그대로 읽고, 금액은 읽을 때 계산한다.
 */
@Entity
@Table(name = "bid_price_inputs",
        uniqueConstraints = @UniqueConstraint(name = "uk_bid_price_inputs_line",
                columnNames = {"bid_request_id", "clin", "period"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class BidPriceInput {

    @Id
    @UuidGenerator
    @Column(name = "id", columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "bid_request_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID bidRequestId;

    @Column(name = "clin", nullable = false, length = 50)
    private String clin;

    @Column(name = "period", nullable = false, length = 200)
    private String period;

    @Column(name = "unit_price", nullable = false, precision = 20, scale = 6)
    private BigDecimal unitPrice;

    @Column(name = "updated_by", nullable = false)
    private String updatedBy;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void changePrice(BigDecimal unitPrice, String updatedBy, LocalDateTime updatedAt) {
        this.unitPrice = unitPrice;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
    }
}
