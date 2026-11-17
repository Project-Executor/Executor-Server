package com.seyoung5309.executorserver.domain.auth.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.seyoung5309.executorserver.global.jpa.CreatedAtSeconds;

import java.time.Instant;

/** 로그인 계정 (이메일·비밀번호·권한). 게임 정보는 users 테이블(User)에 있다 */
@Entity
@Table(name = "accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {

    /** 영구 정지를 나타내는 정지 기한 (MySQL DATETIME 최댓값) */
    public static final Instant PERMANENT = Instant.parse("9999-12-31T23:59:59Z");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    /** BCrypt 해시 */
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @CreatedAtSeconds
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /** 정지 기한. null이면 정지 아님, PERMANENT면 영구 정지 */
    private Instant suspendedUntil;

    @Column(length = 200)
    private String suspendReason;

    public Account(String email, String encodedPassword) {
        this.email = email;
        this.password = encodedPassword;
        this.role = Role.USER;
    }

    /** 관리자 화면에서 권한을 바꾼다. 새 권한은 다음 토큰 재발급(최대 Access Token 유효기간 30분) 때부터 적용된다 */
    public void changeRole(Role role) {
        this.role = role;
    }

    /** now 시점에 정지 중인지. 기한이 지나면 자동으로 풀린 것으로 본다 */
    public boolean isSuspendedAt(Instant now) {
        return suspendedUntil != null && suspendedUntil.isAfter(now);
    }

    /** @param until 정지 기한 (영구면 PERMANENT) */
    public void suspend(Instant until, String reason) {
        this.suspendedUntil = until;
        this.suspendReason = reason;
    }

    public void unsuspend() {
        this.suspendedUntil = null;
        this.suspendReason = null;
    }
}
