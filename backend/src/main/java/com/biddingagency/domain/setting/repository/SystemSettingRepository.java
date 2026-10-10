package com.biddingagency.domain.setting.repository;

import com.biddingagency.domain.setting.entity.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemSettingRepository extends JpaRepository<SystemSetting, String> {
}
