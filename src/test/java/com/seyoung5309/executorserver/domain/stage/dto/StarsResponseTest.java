package com.seyoung5309.executorserver.domain.stage.dto;

import com.seyoung5309.executorserver.domain.stage.entity.Operation;
import com.seyoung5309.executorserver.domain.stage.entity.Stage;
import com.seyoung5309.executorserver.domain.stage.entity.StageType;
import org.junit.jupiter.api.Test;

import static com.seyoung5309.executorserver.domain.stage.StageFixtures.cleared;
import static com.seyoung5309.executorserver.domain.stage.StageFixtures.notCleared;
import static com.seyoung5309.executorserver.domain.stage.StageFixtures.officialStage;
import static com.seyoung5309.executorserver.domain.stage.StageFixtures.operation;
import static org.assertj.core.api.Assertions.assertThat;

class StarsResponseTest {

    private final Operation op = operation(1L, "op", 1);
    private final Stage normal = officialStage(1L, op, 1, StageType.NORMAL, 10);
    private final Stage boss = officialStage(2L, op, 2, StageType.BOSS, 10);
    private final Stage tutorial = officialStage(3L, op, 3, StageType.TUTORIAL, 10);

    @Test
    void 기록이_없으면_별_0개() {
        assertThat(StarsResponse.of(normal, null)).isEqualTo(new StarsResponse(false, false, false, 0, 3));
    }

    @Test
    void 클리어하지_못한_기록은_별_0개() {
        assertThat(StarsResponse.of(normal, notCleared(normal, 4))).isEqualTo(new StarsResponse(false, false, false, 0, 3));
    }

    @Test
    void 클리어만_하면_별_1개() {
        assertThat(StarsResponse.of(normal, cleared(normal, 11, false))).isEqualTo(new StarsResponse(true, false, false, 1, 3));
    }

    @Test
    void 기준_턴과_같으면_별_2_달성() {
        assertThat(StarsResponse.of(normal, cleared(normal, 10, false)).par()).isTrue();
    }

    @Test
    void 기준_턴_이하와_숨겨진_요소를_모두_달성하면_별_3개() {
        assertThat(StarsResponse.of(normal, cleared(normal, 8, true))).isEqualTo(new StarsResponse(true, true, true, 3, 3));
    }

    @Test
    void 보스_스테이지도_별_3개_체계() {
        assertThat(StarsResponse.of(boss, cleared(boss, 8, true)).count()).isEqualTo(3);
    }

    @Test
    void 튜토리얼은_조건을_다_달성해도_별_1개가_최대() {
        assertThat(StarsResponse.of(tutorial, cleared(tutorial, 1, true))).isEqualTo(new StarsResponse(true, false, false, 1, 1));
    }
}
