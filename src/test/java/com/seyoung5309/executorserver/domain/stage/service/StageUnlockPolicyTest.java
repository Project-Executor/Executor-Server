package com.seyoung5309.executorserver.domain.stage.service;

import com.seyoung5309.executorserver.domain.stage.entity.Operation;
import com.seyoung5309.executorserver.domain.stage.entity.Stage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static com.seyoung5309.executorserver.domain.stage.StageFixtures.officialStage;
import static com.seyoung5309.executorserver.domain.stage.StageFixtures.operation;
import static org.assertj.core.api.Assertions.assertThat;

class StageUnlockPolicyTest {

    private final Operation tutorial = operation(1L, "Tutorial", 0);
    private final Operation op1 = operation(2L, "Operation 1", 1);

    // 플레이 순서: 튜토리얼 1 -> 작전1 1 -> 작전1 2 -> 작전1 3
    private final Stage t1 = officialStage(10L, tutorial, 1);
    private final Stage s1 = officialStage(20L, op1, 1);
    private final Stage s2 = officialStage(21L, op1, 2);
    private final Stage s3 = officialStage(22L, op1, 3);
    private final List<Stage> stages = List.of(t1, s1, s2, s3);

    @Test
    void 아무것도_안_깼으면_첫_스테이지만_열린다() {
        assertThat(StageUnlockPolicy.unlockedStageIds(stages, Set.of())).containsExactly(10L);
    }

    @Test
    void 앞_스테이지를_깨면_바로_다음만_열린다() {
        assertThat(StageUnlockPolicy.unlockedStageIds(stages, Set.of(10L)))
                .containsExactlyInAnyOrder(10L, 20L);
    }

    @Test
    void 작전_경계를_넘어서도_순서대로_열린다() {
        assertThat(StageUnlockPolicy.unlockedStageIds(stages, Set.of(10L, 20L, 21L)))
                .containsExactlyInAnyOrder(10L, 20L, 21L, 22L);
    }

    @Test
    void 이미_깬_스테이지는_앞이_안_깨졌어도_열려_있다() {
        // 관리자가 t1과 s1 사이에 새 스테이지를 끼워 넣은 상황
        Stage inserted = officialStage(99L, tutorial, 2);
        List<Stage> withInserted = List.of(t1, inserted, s1, s2, s3);

        Set<Long> unlocked = StageUnlockPolicy.unlockedStageIds(withInserted, Set.of(10L, 20L));

        assertThat(unlocked).containsExactlyInAnyOrder(10L, 99L, 20L, 21L);
        assertThat(unlocked).doesNotContain(22L);
    }

    @Test
    void 스테이지가_없으면_빈_결과() {
        assertThat(StageUnlockPolicy.unlockedStageIds(List.of(), Set.of())).isEmpty();
    }
}
