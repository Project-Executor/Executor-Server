package com.seyoung5309.executorserver.domain.guestbook.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 방명록 글 하나 (전시용, 로그인 없이 이름만 적어 남긴다).
 * 욕설·개인정보는 저장하기 전에 가린다(GuestbookFilter). 로그인한 사용자가 남기면 userId가 있다 (하루 첫 글 코인 보상 판단용).
 */
@Entity
@Table(name = "guestbook_entries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GuestbookEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String name;

    @Column(nullable = false, length = 200)
    private String message;

    /** 로그인하고 남겼으면 사용자 ID (탈퇴하면 NULL) */
    @Column(name = "user_id")
    private Long userId;

    /** 같은 기기 연속 작성 제한용 (게임이 PC마다 만든 임의 값) */
    @Column(name = "device_id", length = 64)
    private String deviceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public GuestbookEntry(String name, String message, Long userId, String deviceId, Instant createdAt) {
        this.name = name;
        this.message = message;
        this.userId = userId;
        this.deviceId = deviceId;
        this.createdAt = createdAt;
    }
}
