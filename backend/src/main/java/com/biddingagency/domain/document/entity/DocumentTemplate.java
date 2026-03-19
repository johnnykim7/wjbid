package com.biddingagency.domain.document.entity;

import com.biddingagency.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

/**
 * 문서 템플릿 엔티티
 * TipTap JSON 형식의 템플릿을 저장합니다.
 * Claude가 문서 생성 시 이 템플릿을 기반으로 작성합니다.
 */
@Entity
@Table(name = "document_templates",
    indexes = {
        @Index(name = "idx_templates_type", columnList = "document_type"),
        @Index(name = "idx_templates_active", columnList = "active")
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DocumentTemplate extends BaseEntity {

    @Column(name = "template_name", nullable = false, length = 255)
    private String templateName;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 50)
    private DocumentType documentType;

    @Column(name = "template_version", nullable = false)
    @Builder.Default
    private Integer templateVersion = 1;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content_json", columnDefinition = "JSON", nullable = false)
    private Map<String, Object> contentJson;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    public void deactivate() {
        this.active = false;
    }
}
