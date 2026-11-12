package com.seyoung5309.executorserver.domain.stage.entity;

import com.seyoung5309.executorserver.domain.auth.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** 사용자가 본 힌트. 행은 UserStageHintRepository.insertIfAbsent로만 만든다 */
@Entity
@Table(name = "user_stage_hints")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserStageHint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stage_id", nullable = false)
    private Stage stage;

    /** map_data.hints 배열 번호 (0부터) */
    @Column(nullable = false)
    private int hintIndex;

    @Column(nullable = false, updatable = false)
    private Instant viewedAt;
}
