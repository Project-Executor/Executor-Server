package com.seyoung5309.executorserver.domain.auth.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.seyoung5309.executorserver.global.jpa.CreatedAtSeconds;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

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

    public User(String email, String encodedPassword) {
        this.email = email;
        this.password = encodedPassword;
        this.role = Role.USER;
    }
}
