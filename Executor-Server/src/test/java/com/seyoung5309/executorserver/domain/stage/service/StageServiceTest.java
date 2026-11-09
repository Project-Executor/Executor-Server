package com.seyoung5309.executorserver.domain.stage.service;

import com.seyoung5309.executorserver.domain.stage.dto.OperationResponse;
import com.seyoung5309.executorserver.domain.stage.dto.StageSummaryResponse;
import com.seyoung5309.executorserver.domain.stage.entity.Operation;
import com.seyoung5309.executorserver.domain.stage.entity.Stage;
import com.seyoung5309.executorserver.domain.stage.entity.StageType;
import com.seyoung5309.executorserver.domain.stage.repository.OperationRepository;
import com.seyoung5309.executorserver.domain.stage.repository.StageRepository;
import com.seyoung5309.executorserver.domain.stage.repository.UserStageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.seyoung5309.executorserver.domain.stage.StageFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StageServiceTest {

    private OperationRepository operationRepository;
    private StageRepository stageRepository;
    private UserStageRepository userStageRepository;
    private StageService stageService;

    private final Operation tutorial = operation(1L, "Tutorial", 0);
    private final Operation op1 = operation(2L, "Operation 1", 1);
    private final Operation op2 = operation(3L, "Operation 2", 2);

    private final Stage t1 = officialStage(10L, tutorial, 1, StageType.TUTORIAL, 30);
    private final Stage s1 = officialStage(20L, op1, 1, StageType.NORMAL, 20);
    private final Stage s2 = officialStage(21L, op1, 2, StageType.BOSS, 25);

    @BeforeEach
    void setUp() {
        operationRepository = mock(OperationRepository.class);
        stageRepository = mock(StageRepository.class);
        userStageRepository = mock(UserStageRepository.class);
        stageService = new StageService(operationRepository, stageRepository, userStageRepository);

        when(operationRepository.findAllByOrderBySortOrderAsc()).thenReturn(List.of(tutorial, op1, op2));
        when(stageRepository.findAllOfficialInPlayOrder()).thenReturn(List.of(t1, s1, s2));
    }

    @Test
    void 작전별로_스테이지를_묶고_스테이지가_없는_작전도_포함한다() {
        when(userStageRepository.findAllOfficialByUserId(1L)).thenReturn(List.of());

        List<OperationResponse> result = stageService.getOperations(1L);

        assertThat(result).extracting(OperationResponse::name).containsExactly("Tutorial", "Operation 1", "Operation 2");
        assertThat(result.get(0).stages()).extracting(StageSummaryResponse::stageId).containsExactly(10L);
        assertThat(result.get(1).stages()).extracting(StageSummaryResponse::stageId).containsExactly(20L, 21L);
        assertThat(result.get(2).stages()).isEmpty();
    }

    @Test
    void 처음_시작한_사용자는_튜토리얼만_열려_있고_기록은_비어_있다() {
        when(userStageRepository.findAllOfficialByUserId(1L)).thenReturn(List.of());

        List<OperationResponse> result = stageService.getOperations(1L);

        StageSummaryResponse tut = result.get(0).stages().get(0);
        assertThat(tut.unlocked()).isTrue();
        assertThat(tut.cleared()).isFalse();
        assertThat(tut.bestMoves()).isNull();
        assertThat(tut.playCount()).isZero();
        assertThat(tut.stars().max()).isEqualTo(1);

        assertThat(result.get(1).stages()).allSatisfy(stage -> assertThat(stage.unlocked()).isFalse());
    }

    @Test
    void 튜토리얼을_깨면_작전1_첫_스테이지가_열리고_기록과_별이_채워진다() {
        when(userStageRepository.findAllOfficialByUserId(1L))
                .thenReturn(List.of(cleared(t1, 34, false), notCleared(s1, 3)));

        List<OperationResponse> result = stageService.getOperations(1L);

        StageSummaryResponse tut = result.get(0).stages().get(0);
        assertThat(tut.cleared()).isTrue();
        assertThat(tut.bestMoves()).isEqualTo(34);
        assertThat(tut.stars().count()).isEqualTo(1);

        StageSummaryResponse first = result.get(1).stages().get(0);
        assertThat(first.unlocked()).isTrue();
        assertThat(first.cleared()).isFalse();
        assertThat(first.playCount()).isEqualTo(3);

        StageSummaryResponse boss = result.get(1).stages().get(1);
        assertThat(boss.unlocked()).isFalse();
        assertThat(boss.stars().max()).isEqualTo(3);
    }
}
