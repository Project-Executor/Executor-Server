package com.seyoung5309.executorserver.domain.stage.service;

import com.seyoung5309.executorserver.domain.auth.entity.User;
import com.seyoung5309.executorserver.domain.stage.entity.Operation;
import com.seyoung5309.executorserver.domain.stage.entity.Stage;
import com.seyoung5309.executorserver.domain.stage.repository.StageRepository;
import com.seyoung5309.executorserver.domain.stage.repository.UserStageRepository;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.seyoung5309.executorserver.domain.stage.StageFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class StageAccessServiceTest {

    private StageRepository stageRepository;
    private UserStageRepository userStageRepository;
    private StageAccessService accessService;

    private final Operation op = operation(1L, "op", 1);
    private final Stage s1 = officialStage(10L, op, 1);
    private final Stage s2 = officialStage(11L, op, 2);
    private final User me = user(1L);
    private final User other = user(2L);

    @BeforeEach
    void setUp() {
        stageRepository = mock(StageRepository.class);
        userStageRepository = mock(UserStageRepository.class);
        accessService = new StageAccessService(stageRepository, userStageRepository);
        when(stageRepository.findAllOfficialInPlayOrder()).thenReturn(List.of(s1, s2));
        when(userStageRepository.findClearedOfficialStageIds(1L)).thenReturn(Set.of());
    }

    @Test
    void 열린_공식_스테이지는_플레이할_수_있다() {
        when(stageRepository.findById(10L)).thenReturn(Optional.of(s1));

        assertThat(accessService.getPlayableStage(1L, 10L)).isSameAs(s1);
    }

    @Test
    void 잠긴_공식_스테이지는_STAGE_LOCKED() {
        when(stageRepository.findById(11L)).thenReturn(Optional.of(s2));

        assertErrorCode(() -> accessService.getPlayableStage(1L, 11L), ErrorCode.STAGE_LOCKED);
    }

    @Test
    void 앞_스테이지를_깨면_다음_스테이지를_플레이할_수_있다() {
        when(stageRepository.findById(11L)).thenReturn(Optional.of(s2));
        when(userStageRepository.findClearedOfficialStageIds(1L)).thenReturn(Set.of(10L));

        assertThat(accessService.getPlayableStage(1L, 11L)).isSameAs(s2);
    }

    @Test
    void 공개된_남의_맵은_플레이할_수_있다() {
        Stage map = userMap(50L, other, true);
        when(stageRepository.findById(50L)).thenReturn(Optional.of(map));

        assertThat(accessService.getPlayableStage(1L, 50L)).isSameAs(map);
        verify(stageRepository, never()).findAllOfficialInPlayOrder();
    }

    @Test
    void 비공개_맵은_제작자만_플레이할_수_있다() {
        Stage map = userMap(51L, me, false);
        when(stageRepository.findById(51L)).thenReturn(Optional.of(map));

        assertThat(accessService.getPlayableStage(1L, 51L)).isSameAs(map);
    }

    @Test
    void 남의_비공개_맵은_FORBIDDEN() {
        Stage map = userMap(52L, other, false);
        when(stageRepository.findById(52L)).thenReturn(Optional.of(map));

        assertErrorCode(() -> accessService.getPlayableStage(1L, 52L), ErrorCode.FORBIDDEN);
    }

    @Test
    void 없는_스테이지는_NOT_FOUND() {
        when(stageRepository.findById(99L)).thenReturn(Optional.empty());

        assertErrorCode(() -> accessService.getPlayableStage(1L, 99L), ErrorCode.NOT_FOUND);
    }

    private void assertErrorCode(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
    }
}
