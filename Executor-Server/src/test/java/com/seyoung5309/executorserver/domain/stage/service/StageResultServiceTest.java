package com.seyoung5309.executorserver.domain.stage.service;

import com.seyoung5309.executorserver.domain.guidebook.entity.Guidebook;
import com.seyoung5309.executorserver.domain.guidebook.entity.GuidebookCategory;
import com.seyoung5309.executorserver.domain.guidebook.repository.GuidebookRepository;
import com.seyoung5309.executorserver.domain.guidebook.repository.UserGuidebookRepository;
import com.seyoung5309.executorserver.domain.profile.repository.ProfileRepository;
import com.seyoung5309.executorserver.domain.stage.dto.StageResultRequest;
import com.seyoung5309.executorserver.domain.stage.dto.StageResultResponse;
import com.seyoung5309.executorserver.domain.stage.entity.Operation;
import com.seyoung5309.executorserver.domain.stage.entity.Stage;
import com.seyoung5309.executorserver.domain.stage.entity.StageType;
import com.seyoung5309.executorserver.domain.stage.entity.UserStage;
import com.seyoung5309.executorserver.domain.stage.repository.StageRepository;
import com.seyoung5309.executorserver.domain.stage.repository.UserStageRepository;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static com.seyoung5309.executorserver.domain.stage.StageFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class StageResultServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-10T03:00:00Z");

    private StageAccessService accessService;
    private StageRepository stageRepository;
    private UserStageRepository userStageRepository;
    private ProfileRepository profileRepository;
    private GuidebookRepository guidebookRepository;
    private UserGuidebookRepository userGuidebookRepository;
    private StageResultService service;

    private final Operation op = operation(1L, "op", 1);
    private final Stage s1 = officialStage(10L, op, 1, StageType.NORMAL, 20); // rewardCoin 50
    private final Stage s2 = officialStage(11L, op, 2, StageType.BOSS, 25);
    private final Stage tutorial = officialStage(9L, op, 0, StageType.TUTORIAL, 30);

    @BeforeEach
    void setUp() {
        accessService = mock(StageAccessService.class);
        stageRepository = mock(StageRepository.class);
        userStageRepository = mock(UserStageRepository.class);
        profileRepository = mock(ProfileRepository.class);
        guidebookRepository = mock(GuidebookRepository.class);
        userGuidebookRepository = mock(UserGuidebookRepository.class);
        service = new StageResultService(accessService, stageRepository, userStageRepository, profileRepository,
                guidebookRepository, userGuidebookRepository, Clock.fixed(NOW, ZoneOffset.UTC));

        when(stageRepository.findAllOfficialInPlayOrder()).thenReturn(List.of(tutorial, s1, s2));
        when(profileRepository.findCoinByUserId(1L)).thenReturn(Optional.of(100));
    }

    @Test
    void 첫_클리어면_보상을_지급하고_다음_스테이지를_알려준다() {
        givenRecord(s1, notCleared(s1, 1));
        when(profileRepository.findCoinByUserId(1L)).thenReturn(Optional.of(150));

        StageResultResponse response = service.submit(1L, 10L, request(true, 18, true));

        verify(userStageRepository).upsertPlay(1L, 10L, NOW);
        verify(profileRepository).addCoin(1L, 50);
        assertThat(response.firstClear()).isTrue();
        assertThat(response.rewardCoin()).isEqualTo(50);
        assertThat(response.totalCoin()).isEqualTo(150);
        assertThat(response.isNewBest()).isTrue();
        assertThat(response.bestMoves()).isEqualTo(18);
        assertThat(response.thisRun()).isEqualTo(new StageResultResponse.RunResult(true, true, true));
        assertThat(response.stars().count()).isEqualTo(3);
        assertThat(response.nextStageId()).isEqualTo(11L);
    }

    @Test
    void 이미_클리어했으면_보상을_다시_주지_않는다() {
        givenRecord(s1, cleared(s1, 18, false));

        StageResultResponse response = service.submit(1L, 10L, request(true, 25, false));

        verify(profileRepository, never()).addCoin(anyLong(), anyInt());
        assertThat(response.firstClear()).isFalse();
        assertThat(response.rewardCoin()).isZero();
        assertThat(response.isNewBest()).isFalse();
        assertThat(response.bestMoves()).isEqualTo(18);
        assertThat(response.thisRun().par()).as("25턴은 기준 20턴 초과").isFalse();
    }

    @Test
    void 클리어하지_못한_판은_도전_횟수만_늘고_기록과_보상은_그대로() {
        UserStage record = notCleared(s1, 3);
        givenRecord(s1, record);

        StageResultResponse response = service.submit(1L, 10L, request(false, null, false));

        verify(userStageRepository).upsertPlay(1L, 10L, NOW);
        verify(profileRepository, never()).addCoin(anyLong(), anyInt());
        assertThat(record.getFirstClearedAt()).isNull();
        assertThat(response.cleared()).isFalse();
        assertThat(response.thisRun()).isEqualTo(new StageResultResponse.RunResult(false, false, false));
        assertThat(response.bestMoves()).isNull();
        assertThat(response.nextStageId()).isNull();
    }

    @Test
    void 클리어하지_못했으면_moves와_hiddenFound는_무시한다() {
        UserStage record = notCleared(s1, 1);
        givenRecord(s1, record);

        service.submit(1L, 10L, request(false, 3, true));

        assertThat(record.getBestMoves()).isNull();
        assertThat(record.isHiddenFound()).isFalse();
    }

    @Test
    void 클리어했는데_moves가_없으면_INVALID_INPUT이고_아무것도_기록하지_않는다() {
        assertErrorCode(() -> service.submit(1L, 10L, request(true, null, false)), ErrorCode.INVALID_INPUT);
        verify(userStageRepository, never()).upsertPlay(anyLong(), anyLong(), any());
    }

    @Test
    void 잠긴_스테이지면_STAGE_LOCKED이고_아무것도_기록하지_않는다() {
        when(accessService.getPlayableStage(1L, 11L)).thenThrow(new BusinessException(ErrorCode.STAGE_LOCKED));

        assertErrorCode(() -> service.submit(1L, 11L, request(true, 10, false)), ErrorCode.STAGE_LOCKED);
        verify(userStageRepository, never()).upsertPlay(anyLong(), anyLong(), any());
    }

    @Test
    void 사용자_맵은_reward_coin이_있어도_보상이_없다() {
        Stage map = userMap(50L, user(2L), true);
        ReflectionTestUtils.setField(map, "rewardCoin", 999);
        givenRecord(map, notCleared(map, 1));

        StageResultResponse response = service.submit(1L, 50L, request(true, 5, false));

        assertThat(response.firstClear()).isTrue();
        assertThat(response.rewardCoin()).isZero();
        assertThat(response.nextStageId()).isNull();
        verify(profileRepository, never()).addCoin(anyLong(), anyInt());
    }

    @Test
    void 튜토리얼은_조건을_다_달성해도_이번_판_별은_클리어만() {
        givenRecord(tutorial, notCleared(tutorial, 1));

        StageResultResponse response = service.submit(1L, 9L, request(true, 1, true));

        assertThat(response.thisRun()).isEqualTo(new StageResultResponse.RunResult(true, false, false));
        assertThat(response.stars().max()).isEqualTo(1);
        assertThat(response.nextStageId()).isEqualTo(10L);
    }

    @Test
    void 마지막_스테이지를_깨면_다음_스테이지는_null() {
        givenRecord(s2, notCleared(s2, 1));

        assertThat(service.submit(1L, 11L, request(true, 10, false)).nextStageId()).isNull();
    }

    @Test
    void 처음_만난_도감만_해금_목록에_들어가고_없는_ID와_중복은_무시한다() {
        givenRecord(s1, notCleared(s1, 1));
        Guidebook npe = guidebook(3L, "NullPointerException");
        Guidebook loop = guidebook(7L, "InfiniteLoop");
        when(guidebookRepository.findAllById(List.of(3L, 7L, 999L))).thenReturn(List.of(npe, loop));
        when(userGuidebookRepository.unlock(1L, 3L, NOW)).thenReturn(1);
        when(userGuidebookRepository.unlock(1L, 7L, NOW)).thenReturn(0); // 이미 해금됨

        StageResultResponse response = service.submit(1L, 10L,
                new StageResultRequest(false, null, null, List.of(3L, 7L, 3L, 999L)));

        assertThat(response.unlockedGuidebooks())
                .containsExactly(new StageResultResponse.UnlockedGuidebook(3L, GuidebookCategory.BUG, "NullPointerException"));
        verify(userGuidebookRepository, never()).unlock(anyLong(), eq(999L), any());
    }

    private void givenRecord(Stage stage, UserStage record) {
        when(accessService.getPlayableStage(1L, stage.getId())).thenReturn(stage);
        when(userStageRepository.findForUpdate(1L, stage.getId())).thenReturn(Optional.of(record));
    }

    private StageResultRequest request(boolean cleared, Integer moves, boolean hidden) {
        return new StageResultRequest(cleared, moves, hidden, null);
    }

    private Guidebook guidebook(Long id, String title) {
        Guidebook guidebook = BeanUtils.instantiateClass(Guidebook.class);
        ReflectionTestUtils.setField(guidebook, "id", id);
        ReflectionTestUtils.setField(guidebook, "category", GuidebookCategory.BUG);
        ReflectionTestUtils.setField(guidebook, "title", title);
        return guidebook;
    }

    private void assertErrorCode(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
    }
}
