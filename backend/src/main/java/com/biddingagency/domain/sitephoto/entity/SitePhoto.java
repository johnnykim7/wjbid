package com.biddingagency.domain.sitephoto.entity;

import com.biddingagency.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 고객 화면 사진 (CR-153, BID-SITE-001)
 *
 * 고객 화면과 함께 배포된 사진은 bundled_path, 관리자가 올린 사진은 storage_url 만 있다.
 * 올린 사진은 저장한 뒤 바꾸지 않으므로 사진 파일 주소를 브라우저가 오래 남겨 둬도 된다(4.5).
 */
@Entity
@Table(name = "site_photos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SitePhoto extends BaseEntity {

    @Column(name = "slot_key", nullable = false, length = 50)
    private String slotKey;

    @Column(name = "bundled_path")
    private String bundledPath;

    @Column(name = "storage_url", length = 500)
    private String storageUrl;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "width", nullable = false)
    private int width;

    @Column(name = "height", nullable = false)
    private int height;

    @Column(name = "original_width")
    private Integer originalWidth;

    @Column(name = "original_height")
    private Integer originalHeight;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "visible", nullable = false)
    private boolean visible;

    public boolean isBundled() {
        return bundledPath != null;
    }

    public void changeVisible(boolean visible) {
        this.visible = visible;
    }
}
