package com.seyoung5309.executorserver.domain.stage.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static com.seyoung5309.executorserver.domain.stage.StageFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;

class UserStageTest {

    private final Stage stage = officialStage(1L, operation(1L, "op", 1), 1);
    private final Instant t1 = Instant.parse("2026-11-10T01:00:00Z");
    private final Instant t2 = Instant.parse("2026-11-10T02:00:00Z");

    @Test
    void 처음_클리어하면_첫_클리어이고_최고_기록이_생긴다() {
        UserStage record = notCleared(stage, 1);

        UserStage.ClearResult result = record.applyClear(15, false, t1);

        assertThat(result).isEqualTo(new UserStage.ClearResult(true, true));
        assertThat(record.getBestMoves()).isEqualTo(15);
        assertThat(record.getFirstClearedAt()).isEqualTo(t1);
    }

    @Test
    void 두_번째_클리어는_첫_클리어가_아니고_첫_클리어_시각도_그대로() {
        UserStage record = notCleared(stage, 1);
        record.applyClear(15, false, t1);

        UserStage.ClearResult result = record.applyClear(12, false, t2);

        assertThat(result.firstClear()).isFalse();
        assertThat(record.getFirstClearedAt()).isEqualTo(t1);
    }

    @Test
    void 더_적은_턴이면_최고_기록_갱신() {
        UserStage record = cleared(stage, 15, false);

        assertThat(record.applyClear(12, false, t2).isNewBest()).isTrue();
        assertThat(record.getBestMoves()).isEqualTo(12);
    }

    @Test
    void 같거나_많은_턴이면_최고_기록_유지() {
        UserStage record = cleared(stage, 15, false);

        assertThat(record.applyClear(15, false, t2).isNewBest()).isFalse();
        assertThat(record.applyClear(20, false, t2).isNewBest()).isFalse();
        assertThat(record.getBestMoves()).isEqualTo(15);
    }

    @Test
    void 숨겨진_요소는_한_번_찾으면_이후에_못_찾아도_유지된다() {
        UserStage record = cleared(stage, 15, true);

        record.applyClear(14, false, t2);

        assertThat(record.isHiddenFound()).isTrue();
    }
}
