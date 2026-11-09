package com.seyoung5309.executorserver.domain.stage.entity;

import com.seyoung5309.executorserver.domain.auth.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * 스테이지. creator가 NULL이면 공식 스테이지, 값이 있으면 사용자 제작 맵.
 */
@Entity
@Table(name = "stages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 사용자 맵은 NULL */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operation_id")
    private Operation operation;

    /** 작전 내 순서 (사용자 맵은 NULL) */
    private Integer stageNo;

    /** 공식 스테이지는 NULL */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    private User creator;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StageType stageType;

    @Column(nullable = false, length = 100)
    private String title;

    /** 맵 데이터 JSON 원문. 서버는 hints 배열만 해석하고 나머지는 그대로 전달한다. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String mapData;

    /** 기준 턴 수 (별 2 조건) */
    @Column(nullable = false)
    private int parMoves;

    /** 첫 클리어 보상 코인 (사용자 맵은 0) */
    @Column(nullable = false)
    private int rewardCoin;

    @Column(nullable = false)
    private boolean isPublic;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    public boolean isOfficial() {
        return creator == null;
    }
}
