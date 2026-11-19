package com.seyoung5309.executorserver.domain.stage.service;

import com.seyoung5309.executorserver.domain.guidebook.entity.Guidebook;
import com.seyoung5309.executorserver.domain.guidebook.entity.GuidebookCategory;
import com.seyoung5309.executorserver.domain.guidebook.repository.GuidebookRepository;
import com.seyoung5309.executorserver.domain.guidebook.repository.UserGuidebookRepository;
import com.seyoung5309.executorserver.domain.profile.repository.UserRepository;
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
    private UserRepository userRepository;
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
        userRepository = mock(UserRepository.class);
        guidebookRepository = mock(GuidebookRepository.class);
        userGuidebookRepository = mock(UserGuidebookRepository.class);
        service = new StageResultService(accessService, stageRepository, userStageRepository, userRepository,
                guidebookRepository, userGuidebookRepository, Clock.fixed(NOW, ZoneOffset.UTC),
                tools.jackson.databind.json.JsonMapper.builder().build());

        when(stageRepository.findAllOfficialInPlayOrder()).thenReturn(List.of(tutorial, s1, s2));
        when(userRepository.findCoinByUserId(1L)).thenReturn(Optional.of(100));
    }

    @Test
    void 첫_클리어면_보상을_지급하고_다음_스테이지를_알려준다() {
        givenRecord(s1, notCleared(s1, 1));
        when(userRepository.findCoinByUserId(1L)).thenReturn(Optional.of(150));

        StageResultResponse response = service.submit(1L, 10L, request(true, 18, true));

        verify(userStageRepository).upsertPlay(1L, 10L, NOW);
        verify(userRepository).addCoin(1L, 50);
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

        StageResultResponse response = service.submit(1L, 10L,
                new StageResultRequest(true, 25, false, null, 1, null, null));

        verify(userRepository, never()).addCoin(anyLong(), anyInt());
        assertThat(response.firstClear()).isFalse();
        assertThat(response.rewardCoin()).isZero();
        assertThat(response.isNewBest()).isFalse();
        assertThat(response.bestMoves()).isEqualTo(18);
        assertThat(response.thisRun().par()).as("불안정 1개 > 허용 0개").isFalse();
    }

    @Test
    void 클리어하지_못한_판은_도전_횟수만_늘고_기록과_보상은_그대로() {
        UserStage record = notCleared(s1, 3);
        givenRecord(s1, record);

        StageResultResponse response = service.submit(1L, 10L, request(false, null, false));

        verify(userStageRepository).upsertPlay(1L, 10L, NOW);
        verify(userRepository, never()).addCoin(anyLong(), anyInt());
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
        verify(userRepository, never()).addCoin(anyLong(), anyInt());
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
                new StageResultRequest(false, null, null, List.of(3L, 7L, 3L, 999L), null, null, null));

        assertThat(response.unlockedGuidebooks())
                .containsExactly(new StageResultResponse.UnlockedGuidebook(3L, GuidebookCategory.BUG, "NullPointerException"));
        verify(userGuidebookRepository, never()).unlock(anyLong(), eq(999L), any());
    }

    private void givenRecord(Stage stage, UserStage record) {
        when(accessService.getPlayableStage(1L, stage.getId())).thenReturn(stage);
        when(userStageRepository.findForUpdate(1L, stage.getId())).thenReturn(Optional.of(record));
    }

    private StageResultRequest request(boolean cleared, Integer moves, boolean hidden) {
        return new StageResultRequest(cleared, moves, hidden, null, null, null, null);
    }

    // ---------- 별 2 (체크포인트 안정·불안정, docs/map-data.md 3.4) ----------

    @Test
    void 불안정_수가_허용_개수_이하면_별_2를_얻고_기록에_남는다() {
        withMapData(s1, "{\"stars\":{\"maxUnstable\":2}}");
        UserStage record = notCleared(s1, 1);
        givenRecord(s1, record);

        StageResultResponse response = service.submit(1L, 10L, new StageResultRequest(true, 40, false, null, 2, 5, null));

        assertThat(response.thisRun().par()).as("기준 턴(20)을 넘겨도 불안정 2 <= 허용 2").isTrue();
        assertThat(response.stars().par()).isTrue();
        assertThat(record.isStableCleared()).isTrue();
    }

    @Test
    void 허용_개수를_넘으면_별_2가_없다() {
        withMapData(s1, "{\"stars\":{\"maxUnstable\":1}}");
        UserStage record = notCleared(s1, 1);
        givenRecord(s1, record);

        StageResultResponse response = service.submit(1L, 10L, new StageResultRequest(true, 10, false, null, 2, null, null));

        assertThat(response.thisRun().par()).isFalse();
        assertThat(response.stars().par()).isFalse();
        assertThat(record.isStableCleared()).isFalse();
    }

    @Test
    void unstableCount를_생략하면_0으로_보고_maxUnstable이_없으면_0이다() {
        UserStage record = notCleared(s1, 1);
        givenRecord(s1, record);

        StageResultResponse response = service.submit(1L, 10L, request(true, 99, false));

        assertThat(response.thisRun().par()).as("체크포인트 없는 맵: 출구까지 안정").isTrue();
        assertThat(record.isStableCleared()).isTrue();
    }

    @Test
    void maxUnstable이_잘못된_값이면_0으로_본다() {
        for (String mapData : List.of("{\"stars\":{\"maxUnstable\":-1}}", "{\"stars\":{\"maxUnstable\":\"3\"}}",
                "{\"stars\":1}", "not json")) {
            assertThat(service.maxUnstable(mapData)).as(mapData).isZero();
        }
        assertThat(service.maxUnstable("{\"stars\":{\"maxUnstable\":3}}")).isEqualTo(3);
    }

    // ---------- 별 개수와 게임 판정 (stars.count, parAchieved) ----------

    @Test
    void 게임이_보낸_parAchieved가_있으면_불안정_수보다_먼저_쓴다() {
        withMapData(s1, "{\"stars\":{\"maxUnstable\":0}}");
        UserStage record = notCleared(s1, 1);
        givenRecord(s1, record);

        // 불안정 5개지만 이 맵의 별 2 조건(예: 기록 n턴 이하)은 게임이 달성으로 판정
        StageResultResponse response = service.submit(1L, 10L, new StageResultRequest(true, 12, false, null, 5, null, true));

        assertThat(response.thisRun().par()).isTrue();
        assertThat(record.isStableCleared()).isTrue();

        UserStage other = notCleared(s1, 1);
        givenRecord(s1, other);
        assertThat(service.submit(1L, 10L, new StageResultRequest(true, 12, false, null, 0, null, false)).thisRun().par())
                .as("불안정 0이어도 게임이 미달성이라고 하면 없음").isFalse();
    }

    @Test
    void 별_수가_2인_맵은_별_3을_주지_않고_최대가_2다() {
        withMapData(s1, "{\"stars\":{\"count\":2}}");
        UserStage record = notCleared(s1, 1);
        givenRecord(s1, record);

        StageResultResponse response = service.submit(1L, 10L, new StageResultRequest(true, 12, true, null, 0, null, true));

        assertThat(response.thisRun().par()).isTrue();
        assertThat(response.thisRun().hidden()).isFalse();
        assertThat(response.stars().max()).isEqualTo(2);
        assertThat(response.stars().count()).isEqualTo(2);
        assertThat(record.isHiddenFound()).isFalse();
    }

    @Test
    void 별_수가_1인_맵은_클리어만() {
        withMapData(s1, "{\"stars\":{\"count\":1}}");
        UserStage record = notCleared(s1, 1);
        givenRecord(s1, record);

        StageResultResponse response = service.submit(1L, 10L, new StageResultRequest(true, 12, true, null, 0, null, true));

        assertThat(response.stars().max()).isEqualTo(1);
        assertThat(response.stars().count()).isEqualTo(1);
        assertThat(record.isStableCleared()).isFalse();
        assertThat(record.isHiddenFound()).isFalse();
    }

    @Test
    void 별_수가_없거나_잘못되면_3() {
        for (String mapData : List.of("{}", "{\"stars\":{\"count\":0}}", "{\"stars\":{\"count\":4}}", "{\"stars\":{\"count\":\"2\"}}", "not json")) {
            assertThat(StarRules.starCount(mapData)).as(mapData).isEqualTo(3);
        }
        assertThat(StarRules.starCount("{\"stars\":{\"count\":2}}")).isEqualTo(2);
    }

    @Test
    void 클리어하지_못한_판은_불안정_수와_상관없이_별_2를_기록하지_않는다() {
        UserStage record = notCleared(s1, 1);
        givenRecord(s1, record);

        StageResultResponse response = service.submit(1L, 10L, new StageResultRequest(false, null, null, null, 0, null, null));

        assertThat(response.thisRun().par()).isFalse();
        assertThat(record.isStableCleared()).isFalse();
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
