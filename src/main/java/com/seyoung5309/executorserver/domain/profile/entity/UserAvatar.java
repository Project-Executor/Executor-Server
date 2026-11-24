package com.seyoung5309.executorserver.domain.profile.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 프로필 사진. 사용자당 하나, 없으면 행이 없다 (게임은 기본 실루엣).
 * 그림이 커서 users(User)와 테이블을 나눴다. 프로필을 읽을 때는 updatedAt(버전)만 따로 읽는다.
 */
@Entity
@Table(name = "user_avatars")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAvatar {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Lob
    @Column(nullable = false, columnDefinition = "MEDIUMBLOB")
    private byte[] image;

    @Column(name = "content_type", nullable = false, length = 20)
    private String contentType;

    /** 바꾼 시각. 게임은 이 값을 그림 캐시 버전으로 쓴다 */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UserAvatar(Long userId, byte[] image, String contentType, Instant now) {
        this.userId = userId;
        change(image, contentType, now);
    }

    public void change(byte[] image, String contentType, Instant now) {
        this.image = image;
        this.contentType = contentType;
        this.updatedAt = now;
    }
}
