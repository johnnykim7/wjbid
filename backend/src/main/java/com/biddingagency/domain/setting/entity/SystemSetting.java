package com.biddingagency.domain.setting.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 운영 설정값 (CR-129)
 *
 * 운영 중 바뀌는 값을 코드 대신 DB 에 둔다. 값은 문자열로 두고 읽는 쪽이 형식을 해석한다.
 * 관리자 입력 화면이 없어 값은 이 행을 직접 갱신해 바꾼다.
 */
@Entity
@Table(name = "system_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SystemSetting {

    @Id
    @Column(name = "setting_key", length = 100)
    private String settingKey;

    @Column(name = "setting_value", length = 500)
    private String settingValue;

    @Column(name = "description", length = 255)
    private String description;

    // DB 가 갱신 시각을 채운다(ON UPDATE CURRENT_TIMESTAMP)
    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
