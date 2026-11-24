package com.seyoung5309.executorserver.domain.admin.service;

import com.seyoung5309.executorserver.domain.admin.dto.AdminStageBoardRequest;
import com.seyoung5309.executorserver.domain.admin.dto.AdminStageBoardRequest.Position;
import com.seyoung5309.executorserver.domain.stage.entity.Operation;
import com.seyoung5309.executorserver.domain.stage.entity.Stage;
import com.seyoung5309.executorserver.domain.stage.repository.LayerRepository;
import com.seyoung5309.executorserver.domain.stage.repository.OperationRepository;
import com.seyoung5309.executorserver.domain.stage.repository.StageRepository;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.seyoung5309.executorserver.domain.stage.StageFixtures.officialStage;
import static com.seyoung5309.executorserver.domain.stage.StageFixtures.operation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/** 스테이지 선택 화면 배치 (PUT /api/admin/operations/{id}/board) */
class AdminOperationBoardTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private OperationRepository operationRepository;
    private StageRepository stageRepository;
    private AdminOperationService service;
    private Operation op;
    private Stage s1, s2;

    @BeforeEach
    void setUp() {
        operationRepository = mock(OperationRepository.class);
        stageRepository = mock(StageRepository.class);
        service = new AdminOperationService(operationRepository, stageRepository, mock(LayerRepository.class));
        op = operation(1L, "Hello World!", 1);
        s1 = officialStage(10L, op, 1);
        s2 = officialStage(11L, op, 2);
        when(operationRepository.findById(1L)).thenReturn(Optional.of(op));
        when(stageRepository.findByOperationIdOrderByStageNoAsc(1L)).thenReturn(List.of(s1, s2));
    }

    private static void assertErrorCode(Runnable call, ErrorCode code) {
        assertThatThrownBy(call::run).isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(code);
    }

    @Test
    void 보낸_스테이지만_위치가_바뀌고_나머지는_그대로() {
        s2.placeOnBoard(0.9, 0.9);

        var res = service.placeStages(1L, new AdminStageBoardRequest(List.of(new Position(10L, 0.25, 0.5))));

        assertThat(s1.getBoardX()).isEqualTo(0.25);
        assertThat(s1.getBoardY()).isEqualTo(0.5);
        assertThat(s2.getBoardX()).isEqualTo(0.9);
        assertThat(res.stages()).extracting(st -> st.boardX()).containsExactly(0.25, 0.9);
        verify(stageRepository).flush();
    }

    @Test
    void 둘_다_null이면_자동_배치로_되돌린다() {
        s1.placeOnBoard(0.3, 0.3);

        service.placeStages(1L, new AdminStageBoardRequest(List.of(new Position(10L, null, null))));

        assertThat(s1.getBoardX()).isNull();
        assertThat(s1.getBoardY()).isNull();
    }

    @Test
    void 다른_작전의_스테이지_중복_한쪽만_null은_INVALID_INPUT() {
        assertErrorCode(() -> service.placeStages(1L, new AdminStageBoardRequest(List.of(new Position(99L, 0.1, 0.1)))),
                ErrorCode.INVALID_INPUT);
        assertErrorCode(() -> service.placeStages(1L, new AdminStageBoardRequest(
                List.of(new Position(10L, 0.1, 0.1), new Position(10L, 0.2, 0.2)))), ErrorCode.INVALID_INPUT);
        assertErrorCode(() -> service.placeStages(1L, new AdminStageBoardRequest(List.of(new Position(11L, 0.1, null)))),
                ErrorCode.INVALID_INPUT);
        verify(stageRepository, never()).flush();
    }

    @Test
    void 없는_작전은_NOT_FOUND() {
        assertErrorCode(() -> service.placeStages(2L, new AdminStageBoardRequest(List.of())), ErrorCode.NOT_FOUND);
    }

    @Test
    void 범위_밖_좌표는_검증에서_막힌다() {
        assertThat(VALIDATOR.validate(new AdminStageBoardRequest(List.of(new Position(10L, 1.2, 0.5))))).isNotEmpty();
        assertThat(VALIDATOR.validate(new AdminStageBoardRequest(List.of(new Position(10L, 0.5, -0.1))))).isNotEmpty();
        assertThat(VALIDATOR.validate(new AdminStageBoardRequest(List.of(new Position(10L, 0.0, 1.0))))).isEmpty();
        assertThat(VALIDATOR.validate(new AdminStageBoardRequest(null))).isNotEmpty();
    }
}
