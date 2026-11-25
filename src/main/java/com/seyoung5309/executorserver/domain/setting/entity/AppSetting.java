package com.seyoung5309.executorserver.domain.setting.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** 서버 설정 하나 (관리자 웹에서 켜고 끄는 값). 예: guestbook.enabled = "true" */
@Entity
@Table(name = "app_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppSetting {

    @Id
    @Column(name = "setting_key", length = 50)
    private String key;

    @Column(name = "setting_value", nullable = false)
    private String value;

    @Column(nullable = false)
    private Instant updatedAt;

    public AppSetting(String key, String value, Instant now) {
        this.key = key;
        change(value, now);
    }

    public void change(String value, Instant now) {
        this.value = value;
        this.updatedAt = now;
    }
}
