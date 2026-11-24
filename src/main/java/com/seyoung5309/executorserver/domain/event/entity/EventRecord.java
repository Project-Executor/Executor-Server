package com.seyoung5309.executorserver.domain.event.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 이벤트 스테이지 기록 한 판. 로그인 없이 이름만 적어 올린다. 같은 스테이지에서 이름은 겹칠 수 없다.
 * score: 턴수 이벤트는 턴(적을수록 위), 타자 이벤트는 친 단어 수(많을수록 위).
 */
@Entity
@Table(name = "event_records")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stage_id", nullable = false)
    private Long stageId;

    @Column(nullable = false, length = 20)
    private String name;

    @Column(nullable = false)
    private int score;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public EventRecord(Long stageId, String name, int score, Instant createdAt) {
        this.stageId = stageId;
        this.name = name;
        this.score = score;
        this.createdAt = createdAt;
    }
}
