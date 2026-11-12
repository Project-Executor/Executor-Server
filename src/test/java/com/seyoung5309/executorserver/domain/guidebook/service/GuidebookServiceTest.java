package com.seyoung5309.executorserver.domain.guidebook.service;

import com.seyoung5309.executorserver.domain.guidebook.dto.GuidebookDetailResponse;
import com.seyoung5309.executorserver.domain.guidebook.dto.GuidebookListResponse;
import com.seyoung5309.executorserver.domain.guidebook.entity.Guidebook;
import com.seyoung5309.executorserver.domain.guidebook.entity.GuidebookCategory;
import com.seyoung5309.executorserver.domain.guidebook.entity.UserGuidebook;
import com.seyoung5309.executorserver.domain.guidebook.repository.GuidebookRepository;
import com.seyoung5309.executorserver.domain.guidebook.repository.UnlockedGuidebookRow;
import com.seyoung5309.executorserver.domain.guidebook.repository.UserGuidebookRepository;
import com.seyoung5309.executorserver.domain.stage.entity.Stage;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.seyoung5309.executorserver.domain.stage.StageFixtures.officialStage;
import static com.seyoung5309.executorserver.domain.stage.StageFixtures.operation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class GuidebookServiceTest {

    private static final Instant UNLOCKED_AT = Instant.parse("2026-11-10T01:00:00Z");

    private GuidebookRepository guidebookRepository;
    private UserGuidebookRepository userGuidebookRepository;
    private GuidebookService service;

    private final Stage stage = officialStage(20L, operation(1L, "op", 1), 1);
    private final Guidebook npe = guidebook(1L, GuidebookCategory.BUG, "NullPointerException", "/g/npe.png", stage);
    private final Guidebook loop = guidebook(2L, GuidebookCategory.BUG, "InfiniteLoop", "/g/loop.png", null);
    private final Guidebook worm = guidebook(3L, GuidebookCategory.VIRUS, "Worm", "/g/worm.png", null);

    @BeforeEach
    void setUp() {
        guidebookRepository = mock(GuidebookRepository.class);
        userGuidebookRepository = mock(UserGuidebookRepository.class);
        service = new GuidebookService(guidebookRepository, userGuidebookRepository);
        when(userGuidebookRepository.findUnlockedByUserId(1L))
                .thenReturn(List.of(new UnlockedGuidebookRow(1L, UNLOCKED_AT), new UnlockedGuidebookRow(3L, UNLOCKED_AT)));
    }

    @Test
    void 전체_목록에서_잠긴_항목은_분류만_보인다() {
        when(guidebookRepository.findAllByOrderByIdAsc()).thenReturn(List.of(npe, loop, worm));

        GuidebookListResponse response = service.getGuidebooks(1L, null);

        assertThat(response.totalCount()).isEqualTo(3);
        assertThat(response.unlockedCount()).isEqualTo(2);
        assertThat(response.items()).containsExactly(
                new GuidebookListResponse.Item(1L, GuidebookCategory.BUG, true, "NullPointerException", "/g/npe.png", UNLOCKED_AT),
                new GuidebookListResponse.Item(2L, GuidebookCategory.BUG, false, null, null, null),
                new GuidebookListResponse.Item(3L, GuidebookCategory.VIRUS, true, "Worm", "/g/worm.png", UNLOCKED_AT));
    }

    @Test
    void 분류를_지정하면_개수도_그_분류_안에서_센다() {
        when(guidebookRepository.findAllByCategoryOrderByIdAsc(GuidebookCategory.BUG)).thenReturn(List.of(npe, loop));

        GuidebookListResponse response = service.getGuidebooks(1L, GuidebookCategory.BUG);

        assertThat(response.totalCount()).isEqualTo(2);
        assertThat(response.unlockedCount()).isEqualTo(1);
    }

    @Test
    void 해금한_항목의_상세에는_설명과_처음_등장_스테이지가_들어간다() {
        when(guidebookRepository.findById(1L)).thenReturn(Optional.of(npe));
        when(userGuidebookRepository.findByUserIdAndGuidebookId(1L, 1L)).thenReturn(Optional.of(unlockedRecord()));

        GuidebookDetailResponse detail = service.getGuidebook(1L, 1L);

        assertThat(detail).isEqualTo(new GuidebookDetailResponse(1L, GuidebookCategory.BUG, "NullPointerException",
                "/g/npe.png", "설명: NullPointerException", 20L, UNLOCKED_AT));
    }

    @Test
    void 처음_등장_스테이지가_없으면_stageId는_null() {
        when(guidebookRepository.findById(3L)).thenReturn(Optional.of(worm));
        when(userGuidebookRepository.findByUserIdAndGuidebookId(1L, 3L)).thenReturn(Optional.of(unlockedRecord()));

        assertThat(service.getGuidebook(1L, 3L).stageId()).isNull();
    }

    @Test
    void 해금하지_않은_항목의_상세는_FORBIDDEN() {
        when(guidebookRepository.findById(2L)).thenReturn(Optional.of(loop));
        when(userGuidebookRepository.findByUserIdAndGuidebookId(1L, 2L)).thenReturn(Optional.empty());

        assertErrorCode(() -> service.getGuidebook(1L, 2L), ErrorCode.FORBIDDEN);
    }

    @Test
    void 없는_항목은_NOT_FOUND() {
        when(guidebookRepository.findById(99L)).thenReturn(Optional.empty());

        assertErrorCode(() -> service.getGuidebook(1L, 99L), ErrorCode.NOT_FOUND);
    }

    @Test
    void 분류는_대소문자를_가리지_않고_비어_있으면_전체() {
        assertThat(GuidebookService.parseCategory("virus")).isEqualTo(GuidebookCategory.VIRUS);
        assertThat(GuidebookService.parseCategory(null)).isNull();
        assertThat(GuidebookService.parseCategory(" ")).isNull();
        assertErrorCode(() -> GuidebookService.parseCategory("monster"), ErrorCode.INVALID_INPUT);
    }

    private Guidebook guidebook(Long id, GuidebookCategory category, String title, String image, Stage stage) {
        Guidebook guidebook = BeanUtils.instantiateClass(Guidebook.class);
        ReflectionTestUtils.setField(guidebook, "id", id);
        ReflectionTestUtils.setField(guidebook, "category", category);
        ReflectionTestUtils.setField(guidebook, "title", title);
        ReflectionTestUtils.setField(guidebook, "image", image);
        ReflectionTestUtils.setField(guidebook, "comment", "설명: " + title);
        ReflectionTestUtils.setField(guidebook, "stage", stage);
        return guidebook;
    }

    private UserGuidebook unlockedRecord() {
        UserGuidebook record = BeanUtils.instantiateClass(UserGuidebook.class);
        ReflectionTestUtils.setField(record, "unlockedAt", UNLOCKED_AT);
        return record;
    }

    private void assertErrorCode(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
    }
}
