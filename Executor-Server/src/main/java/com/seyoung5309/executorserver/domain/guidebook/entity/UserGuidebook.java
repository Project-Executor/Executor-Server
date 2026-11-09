package com.seyoung5309.executorserver.domain.guidebook.entity;

import com.seyoung5309.executorserver.domain.auth.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "user_guidebooks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserGuidebook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guidebook_id", nullable = false)
    private Guidebook guidebook;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant unlockedAt;
}
