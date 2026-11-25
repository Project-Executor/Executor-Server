package com.seyoung5309.executorserver.domain.setting.repository;

import com.seyoung5309.executorserver.domain.setting.entity.AppSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppSettingRepository extends JpaRepository<AppSetting, String> {
}
