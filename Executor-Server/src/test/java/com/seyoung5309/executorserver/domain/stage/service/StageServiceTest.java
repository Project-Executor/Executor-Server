package com.seyoung5309.executorserver.domain.stage.service;

import com.seyoung5309.executorserver.domain.auth.entity.User;
import com.seyoung5309.executorserver.domain.profile.entity.Profile;
import com.seyoung5309.executorserver.domain.profile.repository.ProfileRepository;
import com.seyoung5309.executorserver.domain.stage.dto.MyRecordResponse;
import com.seyoung5309.executorserver.domain.stage.dto.OperationResponse;
import com.seyoung5309.executorserver.domain.stage.dto.StageDetailResponse;
import com.seyoung5309.executorserver.domain.stage.dto.StageSummaryResponse;
import com.seyoung5309.executorserver.domain.stage.dto.StarsResponse;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import tools.jackson.databind.json.JsonMapper;
import com.seyoung5309.executorserver.domain.stage.entity.Operation;
import com.seyoung5309.executorserver.domain.stage.entity.Stage;
import com.seyoung5309.executorserver.domain.stage.entity.StageType;
import com.seyoung5309.executorserver.domain.stage.repository.OperationRepository;
import com.seyoung5309.executorserver.domain.stage.repository.StageRepository;
import com.seyoung5309.executorserver.domain.stage.repository.UserStageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.seyoung5309.executorserver.domain.stage.StageFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StageServiceTest {

    private OperationRepository operationRepository;
    private StageRepository stageRepository;
    private UserStageRepository userStageRepository;
    private ProfileRepository profileRepository;
    private StageAccessService stageAccessService;
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
        profileRepository = mock(ProfileRepository.class);
        stageAccessService = mock(StageAccessService.class);
        stageService = new StageService(operationRepository, stageRepository, userStageRepository,
                profileRepository, stageAccessService, new MapDataParser(JsonMapper.builder().build()));

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

    // ---------- 상세 ----------

    @Test
    void 공식_스테이지_상세는_hints를_빼고_내_기록을_담는다() {
        withMapData(s1, "{\"width\":7,\"hints\":[\"a\",\"b\"]}");
        when(stageAccessService.getPlayableStage(1L, 20L)).thenReturn(s1);
        when(userStageRepository.findByUserIdAndStageId(1L, 20L)).thenReturn(Optional.of(cleared(s1, 15, true)));

        StageDetailResponse detail = stageService.getStageDetail(1L, 20L);

        assertThat(detail.stageId()).isEqualTo(20L);
        assertThat(detail.operationId()).isEqualTo(2L);
        assertThat(detail.creator()).isNull();
        assertThat(detail.hintCount()).isEqualTo(2);
        assertThat(detail.mapData().has("hints")).isFalse();
        assertThat(detail.mapData().get("width").asInt()).isEqualTo(7);
        assertThat(detail.myRecord().cleared()).isTrue();
        assertThat(detail.myRecord().bestMoves()).isEqualTo(15);
        assertThat(detail.myRecord().stars().count()).isEqualTo(3);
    }

    @Test
    void 처음_보는_스테이지는_빈_기록() {
        when(stageAccessService.getPlayableStage(1L, 20L)).thenReturn(s1);
        when(userStageRepository.findByUserIdAndStageId(1L, 20L)).thenReturn(Optional.empty());

        StageDetailResponse detail = stageService.getStageDetail(1L, 20L);

        assertThat(detail.myRecord()).isEqualTo(new MyRecordResponse(false,
                new StarsResponse(false, false, false, 0, 3), null, 0));
    }

    @Test
    void 사용자_맵_상세에는_제작자가_들어가고_작전은_null() {
        User creator = user(7L);
        Stage map = userMap(50L, creator, true);
        when(stageAccessService.getPlayableStage(1L, 50L)).thenReturn(map);
        when(userStageRepository.findByUserIdAndStageId(1L, 50L)).thenReturn(Optional.empty());
        when(profileRepository.findById(7L)).thenReturn(Optional.of(new Profile(creator, "maker")));

        StageDetailResponse detail = stageService.getStageDetail(1L, 50L);

        assertThat(detail.operationId()).isNull();
        assertThat(detail.creator().nickname()).isEqualTo("maker");
    }

    @Test
    void 접근할_수_없으면_예외를_그대로_전달한다() {
        when(stageAccessService.getPlayableStage(1L, 21L)).thenThrow(new BusinessException(ErrorCode.STAGE_LOCKED));

        assertThatThrownBy(() -> stageService.getStageDetail(1L, 21L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.STAGE_LOCKED);
    }
}
