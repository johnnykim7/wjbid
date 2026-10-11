package com.biddingagency.domain.base.entity;

import com.biddingagency.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * 기지 목록 (CR-136, BID-MKT-010)
 *
 * 공고 제목에서 기지를 찾고(BIZ-031) 기지별 지도에 점을 찍는 기준이다. 사람이 관리자 화면에서 채운다.
 * 공고와 기지의 연결은 저장하지 않고 조회할 때 제목으로 찾으므로, 찾을 낱말을 고치면 모든 공고에 바로 적용된다.
 */
@Entity
@Table(name = "military_bases")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MilitaryBase extends BaseEntity {

    /** match_terms 칸에서 낱말을 나누는 문자 */
    public static final String TERM_SEPARATOR = ",";

    @Column(name = "name_ko", nullable = false, length = 100)
    private String nameKo;

    @Column(name = "name_en", nullable = false, length = 100, unique = true)
    private String nameEn;

    /** 제목에서 찾을 낱말, 쉼표로 나눔 */
    @Column(name = "match_terms", nullable = false, length = 1000)
    private String matchTerms;

    @Column(name = "region", length = 100)
    private String region;

    @Column(name = "latitude", precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public List<String> terms() {
        return Arrays.stream(matchTerms.split(TERM_SEPARATOR))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public void update(String nameKo, String nameEn, String matchTerms, String region,
                       BigDecimal latitude, BigDecimal longitude, int sortOrder) {
        this.nameKo = nameKo;
        this.nameEn = nameEn;
        this.matchTerms = matchTerms;
        this.region = region;
        this.latitude = latitude;
        this.longitude = longitude;
        this.sortOrder = sortOrder;
    }
}
