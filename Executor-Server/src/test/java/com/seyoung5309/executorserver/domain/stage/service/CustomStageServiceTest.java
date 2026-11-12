package com.seyoung5309.executorserver.domain.stage.service;

import com.seyoung5309.executorserver.domain.auth.repository.UserRepository;
import com.seyoung5309.executorserver.domain.profile.entity.Profile;
import com.seyoung5309.executorserver.domain.profile.repository.ProfileRepository;
import com.seyoung5309.executorserver.domain.stage.dto.CustomStageRequest;
import com.seyoung5309.executorserver.domain.stage.dto.CustomStageStateResponse;
import com.seyoung5309.executorserver.domain.stage.dto.PublicCustomStageResponse;
import com.seyoung5309.executorserver.domain.stage.entity.Stage;
import com.seyoung5309.executorserver.domain.stage.repository.StageClearCount;
import com.seyoung5309.executorserver.domain.stage.repository.StageRepository;
import com.seyoung5309.executorserver.domain.stage.repository.UserStageHintRepository;
import com.seyoung5309.executorserver.domain.stage.repository.UserStageRepository;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.seyoung5309.executorserver.domain.stage.StageFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CustomStageServiceTest {

    private static final Long ME = 1L;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private StageRepository stageRepository;
    private UserStageRepository userStageRepository;
    private UserStageHintRepository userStageHintRepository;
    private ProfileRepository profileRepository;
    private CustomStageService service;

    @BeforeEach
    void setUp() {
        stageRepository = mock(StageRepository.class);
        userStageRepository = mock(UserStageRepository.class);
        userStageHintRepository = mock(UserStageHintRepository.class);
        profileRepository = mock(ProfileRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        when(userRepository.getReferenceById(anyLong())).thenAnswer(inv -> user(inv.getArgument(0)));
        when(stageRepository.save(any())).thenAnswer(inv -> {
            Stage saved = inv.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 101L);
            return saved;
        });
        service = new CustomStageService(stageRepository, userStageRepository, userStageHintRepository,
                profileRepository, userRepository,
                new MapDataParser(objectMapper));
    }

    @Test
    void 저장하면_비공개_보상_0인_사용자_맵이_된다() {
        CustomStageStateResponse response = service.create(ME, request(" 무한 루프 ", "{\"w\":3,\"hints\":[\"a\"]}"));

        assertThat(response).isEqualTo(new CustomStageStateResponse(101L, false, false));
        ArgumentCaptor<Stage> saved = ArgumentCaptor.forClass(Stage.class);
        verify(stageRepository).save(saved.capture());
        Stage stage = saved.getValue();
        assertThat(stage.isOfficial()).isFalse();
        assertThat(stage.isPublic()).isFalse();
        assertThat(stage.getRewardCoin()).isZero();
        assertThat(stage.getOperation()).isNull();
        assertThat(stage.getStageNo()).isNull();
        assertThat(stage.getTitle()).isEqualTo("무한 루프");
        assertThat(stage.getCreator().getId()).isEqualTo(ME);
    }

    @Test
    void mapData가_객체가_아니거나_256KB를_넘으면_INVALID_INPUT() {
        assertErrorCode(() -> service.create(ME, request("t", "[1]")), ErrorCode.INVALID_INPUT);
        assertErrorCode(() -> service.create(ME, request("t", "{\"hints\":{}}")), ErrorCode.INVALID_INPUT);
        String big = "{\"tiles\":\"" + "a".repeat(256 * 1024) + "\"}";
        assertThatThrownBy(() -> service.create(ME, request("t", big)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("256KB");
        verify(stageRepository, never()).save(any());
    }

    @Test
    void 비공개_맵을_수정하면_기록과_힌트_열람_기록을_지운다() {
        Stage map = userMap(30L, user(ME), false);
        when(stageRepository.findForUpdate(30L)).thenReturn(Optional.of(map));

        CustomStageStateResponse response = service.update(ME, 30L, request("새 제목", "{\"w\":4}"));

        assertThat(response).isEqualTo(new CustomStageStateResponse(30L, false, false));
        assertThat(map.getTitle()).isEqualTo("새 제목");
        assertThat(map.getMapData()).isEqualTo("{\"w\":4}");
        InOrder order = inOrder(stageRepository, userStageRepository);
        order.verify(stageRepository).findForUpdate(30L);
        order.verify(userStageRepository).deleteByStageId(30L);
        verify(userStageHintRepository).deleteByStageId(30L);
    }

    @Test
    void 공개된_맵은_수정할_수_없다() {
        when(stageRepository.findForUpdate(30L)).thenReturn(Optional.of(userMap(30L, user(ME), true)));

        assertErrorCode(() -> service.update(ME, 30L, request("t", "{}")), ErrorCode.PUBLISHED_STAGE);
        verify(userStageRepository, never()).deleteByStageId(anyLong());
        verify(userStageHintRepository, never()).deleteByStageId(anyLong());
    }

    @Test
    void 남의_맵은_FORBIDDEN이고_공식_스테이지나_없는_맵은_NOT_FOUND() {
        when(stageRepository.findForUpdate(30L)).thenReturn(Optional.of(userMap(30L, user(7L), false)));
        when(stageRepository.findForUpdate(20L)).thenReturn(Optional.of(officialStage(20L, operation(1L, "op", 1), 1)));

        assertErrorCode(() -> service.update(ME, 30L, request("t", "{}")), ErrorCode.FORBIDDEN);
        assertErrorCode(() -> service.changeVisibility(ME, 30L, true), ErrorCode.FORBIDDEN);
        assertErrorCode(() -> service.delete(ME, 30L), ErrorCode.FORBIDDEN);
        assertErrorCode(() -> service.delete(ME, 20L), ErrorCode.NOT_FOUND);
        assertErrorCode(() -> service.delete(ME, 99L), ErrorCode.NOT_FOUND);
        verify(stageRepository, never()).delete(any());
    }

    @Test
    void 클리어하지_않은_맵은_공개할_수_없다() {
        Stage map = userMap(30L, user(ME), false);
        when(stageRepository.findForUpdate(30L)).thenReturn(Optional.of(map));
        when(userStageRepository.existsByUserIdAndStageIdAndFirstClearedAtIsNotNull(ME, 30L)).thenReturn(false);

        assertErrorCode(() -> service.changeVisibility(ME, 30L, true), ErrorCode.NOT_VERIFIED);
        assertThat(map.isPublic()).isFalse();
    }

    @Test
    void 클리어한_맵은_공개할_수_있고_비공개로는_언제든_바꿀_수_있다() {
        Stage map = userMap(30L, user(ME), false);
        when(stageRepository.findForUpdate(30L)).thenReturn(Optional.of(map));
        when(userStageRepository.existsByUserIdAndStageIdAndFirstClearedAtIsNotNull(ME, 30L)).thenReturn(true);

        assertThat(service.changeVisibility(ME, 30L, true)).isEqualTo(new CustomStageStateResponse(30L, true, true));
        assertThat(map.isPublic()).isTrue();

        when(userStageRepository.existsByUserIdAndStageIdAndFirstClearedAtIsNotNull(ME, 30L)).thenReturn(false);
        assertThat(service.changeVisibility(ME, 30L, false).isPublic()).isFalse();
    }

    @Test
    void 공개_목록에_클리어_수와_내_클리어_여부를_붙인다() {
        Stage a = userMap(30L, user(7L), true);
        Stage b = userMap(31L, user(8L), true);
        when(stageRepository.findPublicUserMaps(any())).thenReturn(new PageImpl<>(List.of(a, b)));
        when(userStageRepository.countClears(List.of(30L, 31L))).thenReturn(List.of(new StageClearCount(30L, 14)));
        when(userStageRepository.findClearedStageIdsAmong(ME, List.of(30L, 31L))).thenReturn(Set.of(31L));
        when(profileRepository.findAllById(any())).thenReturn(List.of(profile(7L, "nullptr"), profile(8L, "segfault")));

        List<PublicCustomStageResponse> content = service.getPublicMaps(ME, 0, 20).content();

        assertThat(content).extracting(PublicCustomStageResponse::stageId, PublicCustomStageResponse::clearCount,
                        PublicCustomStageResponse::myCleared, r -> r.creator().nickname())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(30L, 14L, false, "nullptr"),
                        org.assertj.core.groups.Tuple.tuple(31L, 0L, true, "segfault"));
    }

    @Test
    void 내_맵_목록에는_클리어_여부가_붙는다() {
        when(stageRepository.findUserMapsByCreator(ME))
                .thenReturn(List.of(userMap(30L, user(ME), true), userMap(31L, user(ME), false)));
        when(userStageRepository.findClearedStageIdsAmong(ME, List.of(30L, 31L))).thenReturn(Set.of(30L));

        assertThat(service.getMyMaps(ME)).extracting(r -> r.stageId(), r -> r.isPublic(), r -> r.verified())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(30L, true, true),
                        org.assertj.core.groups.Tuple.tuple(31L, false, false));
    }

    private CustomStageRequest request(String title, String mapData) {
        return new CustomStageRequest(title, 25, objectMapper.readTree(mapData));
    }

    private static Profile profile(Long userId, String nickname) {
        Profile profile = new Profile(user(userId), nickname);
        ReflectionTestUtils.setField(profile, "userId", userId);
        return profile;
    }

    private static void assertErrorCode(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
    }
}
