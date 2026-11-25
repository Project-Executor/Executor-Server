package com.seyoung5309.executorserver.domain.stage.entity;

import com.seyoung5309.executorserver.domain.auth.entity.Account;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.seyoung5309.executorserver.global.jpa.CreatedAtSeconds;
import org.hibernate.annotations.JdbcTypeCode;
import com.seyoung5309.executorserver.global.jpa.UpdatedAtSeconds;
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
    private Account creator;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StageType stageType;

    @Column(nullable = false, length = 100)
    private String title;

    /** 맵 데이터 JSON 원문. 서버는 해석하지 않고 그대로 전달한다 (구조: docs/map-data.md). */
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

    /** 스테이지 선택 기판 위 노드 가운데 위치 (0~1, 왼쪽 위 기준). 둘 다 NULL이면 게임이 자동 배치한다 (공식 스테이지만 쓴다) */
    @Column(name = "board_x") // 이름 규칙이 boardX를 boardx로 바꾸므로 직접 적는다
    private Double boardX;

    @Column(name = "board_y")
    private Double boardY;

    @CreatedAtSeconds
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdatedAtSeconds
    @Column(nullable = false)
    private Instant updatedAt;

    /** 공식 스테이지를 만든다 (제작자 없음, 항상 공개) */
    public static Stage official(Operation operation, int stageNo, StageType stageType, String title,
                                 String mapData, int parMoves, int rewardCoin) {
        Stage stage = new Stage();
        stage.isPublic = true;
        stage.updateOfficial(operation, stageNo, stageType, title, mapData, parMoves, rewardCoin);
        return stage;
    }

    /** 공식 스테이지 내용을 바꾼다. 사용자들의 기록(user_stages)은 그대로 둔다. */
    public void updateOfficial(Operation operation, int stageNo, StageType stageType, String title,
                               String mapData, int parMoves, int rewardCoin) {
        this.operation = operation;
        this.stageNo = stageNo;
        this.stageType = stageType;
        this.title = title;
        this.mapData = mapData;
        this.parMoves = parMoves;
        this.rewardCoin = rewardCoin;
    }

    /** 사용자 맵을 만든다 (작전·번호 없음, 보상 0, 비공개, 일반 스테이지) */
    public static Stage userMap(Account creator, String title, String mapData, int parMoves) {
        Stage stage = new Stage();
        stage.creator = creator;
        stage.stageType = StageType.NORMAL;
        stage.rewardCoin = 0;
        stage.isPublic = false;
        stage.updateUserMap(title, mapData, parMoves);
        return stage;
    }

    /** 사용자 맵 내용을 바꾼다. 공개 여부 검사와 기록 삭제는 서비스에서 한다. */
    public void updateUserMap(String title, String mapData, int parMoves) {
        this.title = title;
        this.mapData = mapData;
        this.parMoves = parMoves;
    }

    /** 스테이지 선택 기판 위치를 정한다. 둘 다 null이면 자동 배치로 되돌린다. 범위 검사는 서비스에서 한다. */
    public void placeOnBoard(Double x, Double y) {
        this.boardX = x;
        this.boardY = y;
    }

    /** 공개 여부를 바꾼다. 공개 조건(제작자 클리어) 검사는 서비스에서 한다. */
    public void changeVisibility(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public boolean isOfficial() {
        return creator == null;
    }

    /** 비공개 레이어(sortOrder 음수)에 속한 공식 스테이지인지. 게임에서는 없는 스테이지로 다룬다 */
    public boolean isHidden() {
        return isOfficial() && operation != null && operation.getLayer() != null && operation.getLayer().isHidden();
    }
}
