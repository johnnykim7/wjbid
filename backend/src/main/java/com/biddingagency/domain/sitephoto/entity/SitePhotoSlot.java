package com.biddingagency.domain.sitephoto.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 고객 화면 사진 자리 (CR-153, BID-SITE-001)
 *
 * 자리 목록과 이름은 표에 두고, 고객 포털 코드는 화면 칸마다 자리 키만 안다.
 */
@Entity
@Table(name = "site_photo_slots")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class SitePhotoSlot {

    @Id
    @Column(name = "slot_key", length = 50)
    private String slotKey;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "location", nullable = false)
    private String location;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
