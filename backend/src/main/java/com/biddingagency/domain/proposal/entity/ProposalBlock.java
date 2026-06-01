package com.biddingagency.domain.proposal.entity;

import com.biddingagency.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.UUID;

/**
 * Proposal Block — 단락/표/근거 (CR-027). TipTap node 단위.
 *
 * sourceEvidence: 어떤 client doc/sample 에서 가져왔는지 (충실성 추적 — CR-031 검증 기반).
 */
@Entity
@Table(name = "proposal_block",
        indexes = {
                @Index(name = "idx_proposal_block_section", columnList = "section_id"),
                @Index(name = "idx_proposal_block_order", columnList = "section_id, order_no")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ProposalBlock extends BaseEntity {

    @Column(name = "section_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID sectionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "block_type", nullable = false, length = 20)
    @Builder.Default
    private BlockType blockType = BlockType.PARAGRAPH;

    /** TipTap node */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content_json", columnDefinition = "JSON")
    private Map<String, Object> contentJson;

    /** 어떤 client doc/sample 에서 가져왔는지 */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "source_evidence", columnDefinition = "JSON")
    private Map<String, Object> sourceEvidence;

    @Column(name = "order_no", nullable = false)
    @Builder.Default
    private Integer orderNo = 0;

    public void updateOrder(int orderNo) {
        this.orderNo = orderNo;
    }
}
