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

    public Account(String email, String encodedPassword) {
        this.email = email;
        this.password = encodedPassword;
        this.role = Role.USER;
    }
}
