package com.seyoung5309.executorserver.domain.setting.service;

import com.seyoung5309.executorserver.domain.setting.entity.AppSetting;
import com.seyoung5309.executorserver.domain.setting.repository.AppSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/** 관리자 웹에서 켜고 끄는 서버 설정. 값이 없으면 꺼진 것으로 본다 */
@Service
@RequiredArgsConstructor
public class AppSettingService {

    /** 방명록을 게임에 보일지 (전시할 때만 켠다) */
    public static final String GUESTBOOK_ENABLED = "guestbook.enabled";

    private final AppSettingRepository appSettingRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public boolean isOn(String key) {
        return appSettingRepository.findById(key).map(s -> "true".equals(s.getValue())).orElse(false);
    }

    @Transactional
    public void set(String key, boolean on) {
        String value = Boolean.toString(on);
        appSettingRepository.findById(key).ifPresentOrElse(
                s -> s.change(value, clock.instant()),
                () -> appSettingRepository.save(new AppSetting(key, value, clock.instant())));
    }
}
