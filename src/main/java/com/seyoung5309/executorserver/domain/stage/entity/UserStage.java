package com.seyoung5309.executorserver.domain.stage.entity;

import com.seyoung5309.executorserver.domain.auth.entity.Account;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 사용자의 스테이지 기록.
 * 별 1 = firstClearedAt != null, 별 2 = isStableCleared, 별 3 = isHiddenFound (튜토리얼은 별 1만)
 */
@Entity
@Table(name = "user_stages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserStage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Account user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stage_id", nullable = false)
    private Stage stage;

    /** 클리어 시 최소 행동 횟수 (미클리어 NULL) */
    private Integer bestMoves;

    @Column(nullable = false)
    private boolean isHiddenFound;

    /** 불안정 체크포인트가 허용 개수 이하로 클리어한 적 있음 (별 2). 한 번 달성하면 유지 */
    @Column(nullable = false)
    private boolean isStableCleared;

    @Column(nullable = false)
    private int playCount;

    /** NULL -> 값으로 바뀌는 순간 보상 지급 */
    private Instant firstClearedAt;

    @Column(nullable = false)
    private Instant lastPlayedAt;

    public boolean isCleared() {
        return firstClearedAt != null;
    }

    /**
     * 클리어 결과를 기록에 반영한다. 행 잠금(SELECT ... FOR UPDATE)을 건 상태에서 호출해야 한다.
     * <ul>
     *   <li>bestMoves: 더 적은 턴이면 갱신</li>
     *   <li>isHiddenFound, isStableCleared: 한 번 달성하면 유지 (누적)</li>
     *   <li>firstClearedAt: 처음 클리어할 때만 기록 -> 이때만 보상을 지급한다</li>
     * </ul>
     */
    public ClearResult applyClear(int moves, boolean hiddenFound, boolean stable, Instant now) {
        boolean isNewBest = bestMoves == null || moves < bestMoves;
        if (isNewBest) {
            bestMoves = moves;
        }
        if (hiddenFound) {
            isHiddenFound = true;
        }
        if (stable) {
            isStableCleared = true;
        }
        boolean firstClear = firstClearedAt == null;
        if (firstClear) {
            firstClearedAt = now;
        }
        return new ClearResult(isNewBest, firstClear);
    }

    public record ClearResult(boolean isNewBest, boolean firstClear) {
    }
}
