package com.seyoung5309.executorserver.domain.stage.service;

import com.seyoung5309.executorserver.domain.item.repository.UserItemRepository;
import com.seyoung5309.executorserver.domain.profile.repository.ProfileRepository;
import com.seyoung5309.executorserver.domain.stage.dto.HintResponse;
import com.seyoung5309.executorserver.domain.stage.entity.Stage;
import com.seyoung5309.executorserver.domain.stage.repository.UserStageHintRepository;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static com.seyoung5309.executorserver.domain.stage.StageFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class StageHintServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long STAGE_ID = 20L;
    private static final Instant NOW = Instant.parse("2026-11-12T03:00:00Z");

    private StageAccessService stageAccessService;
    private UserItemRepository userItemRepository;
    private UserStageHintRepository userStageHintRepository;
    private ProfileRepository profileRepository;
    private StageHintService service;

    @BeforeEach
    void setUp() {
        stageAccessService = mock(StageAccessService.class);
        userItemRepository = mock(UserItemRepository.class);
        userStageHintRepository = mock(UserStageHintRepository.class);
        profileRepository = mock(ProfileRepository.class);
        service = new StageHintService(stageAccessService, new MapDataParser(JsonMapper.builder().build()),
                userItemRepository, userStageHintRepository, profileRepository, Clock.fixed(NOW, ZoneOffset.UTC));
        when(profileRepository.lockOne(USER_ID)).thenReturn(Optional.of(USER_ID));
        // 기본은 처음 보는 힌트
        when(userStageHintRepository.insertIfAbsent(anyLong(), anyLong(), anyInt(), any())).thenReturn(1);
    }

    @Test
    void 힌트_아이템을_1개_쓰고_해당_번호의_힌트를_받는다() {
        givenStage("{\"w\":3,\"hints\":[\"열쇠는 오른쪽 벽 너머에 있다.\",\"봇은 두 칸마다 움직인다.\"]}");
        when(userItemRepository.useOneHint(USER_ID)).thenReturn(1);
        when(userItemRepository.sumHintQuantity(USER_ID)).thenReturn(2L);

        HintResponse response = service.useHint(USER_ID, STAGE_ID, 1);

        assertThat(response.hintIndex()).isEqualTo(1);
        assertThat(response.content().asString()).isEqualTo("봇은 두 칸마다 움직인다.");
        assertThat(response.charged()).isTrue();
        assertThat(response.remainingHintItems()).isEqualTo(2L);
        InOrder order = inOrder(profileRepository, userStageHintRepository, userItemRepository);
        order.verify(profileRepository).lockOne(USER_ID);
        order.verify(userStageHintRepository).insertIfAbsent(USER_ID, STAGE_ID, 1, NOW);
        order.verify(userItemRepository).useOneHint(USER_ID);
    }

    @Test
    void 이미_본_힌트는_아이템을_쓰지_않고_아이템이_0개여도_받는다() {
        givenStage("{\"hints\":[\"a\",\"b\"]}");
        when(userStageHintRepository.insertIfAbsent(USER_ID, STAGE_ID, 0, NOW)).thenReturn(0);
        when(userItemRepository.sumHintQuantity(USER_ID)).thenReturn(0L);

        HintResponse response = service.useHint(USER_ID, STAGE_ID, 0);

        assertThat(response.content().asString()).isEqualTo("a");
        assertThat(response.charged()).isFalse();
        assertThat(response.remainingHintItems()).isZero();
        verify(userItemRepository, never()).useOneHint(anyLong());
    }

    @Test
    void 힌트가_객체여도_그대로_전달한다() {
        givenStage("{\"hints\":[{\"text\":\"오른쪽\",\"x\":3,\"y\":1}]}");
        when(userItemRepository.useOneHint(USER_ID)).thenReturn(1);

        HintResponse response = service.useHint(USER_ID, STAGE_ID, 0);

        assertThat(response.content().get("text").asString()).isEqualTo("오른쪽");
        assertThat(response.content().get("x").asInt()).isEqualTo(3);
    }

    @Test
    void 힌트_아이템이_없으면_NOT_ENOUGH_ITEM() {
        givenStage("{\"hints\":[\"a\"]}");
        when(userItemRepository.useOneHint(USER_ID)).thenReturn(0);

        assertErrorCode(() -> service.useHint(USER_ID, STAGE_ID, 0), ErrorCode.NOT_ENOUGH_ITEM);
        verify(userItemRepository, never()).sumHintQuantity(anyLong());
    }

    @Test
    void 범위를_벗어난_번호는_아이템을_쓰기_전에_INVALID_INPUT() {
        givenStage("{\"hints\":[\"a\",\"b\"]}");

        assertErrorCode(() -> service.useHint(USER_ID, STAGE_ID, 2), ErrorCode.INVALID_INPUT);
        assertErrorCode(() -> service.useHint(USER_ID, STAGE_ID, -1), ErrorCode.INVALID_INPUT);
        verify(userItemRepository, never()).useOneHint(anyLong());
        verify(userStageHintRepository, never()).insertIfAbsent(anyLong(), anyLong(), anyInt(), any());
    }

    @Test
    void 힌트가_없는_스테이지는_INVALID_INPUT() {
        givenStage("{\"w\":3}");

        assertThatThrownBy(() -> service.useHint(USER_ID, STAGE_ID, 0))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("힌트가 없습니다");
        verify(userItemRepository, never()).useOneHint(anyLong());
    }

    @Test
    void 잠긴_스테이지면_접근_검사의_오류를_그대로_던지고_아이템을_쓰지_않는다() {
        when(stageAccessService.getPlayableStage(USER_ID, STAGE_ID))
                .thenThrow(new BusinessException(ErrorCode.STAGE_LOCKED));

        assertErrorCode(() -> service.useHint(USER_ID, STAGE_ID, 0), ErrorCode.STAGE_LOCKED);
        verify(userItemRepository, never()).useOneHint(anyLong());
    }

    private void givenStage(String mapData) {
        Stage stage = withMapData(officialStage(STAGE_ID, operation(1L, "op", 1), 1), mapData);
        when(stageAccessService.getPlayableStage(USER_ID, STAGE_ID)).thenReturn(stage);
    }

    private void assertErrorCode(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
    }
}
