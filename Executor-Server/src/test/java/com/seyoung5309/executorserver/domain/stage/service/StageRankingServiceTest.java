package com.seyoung5309.executorserver.domain.stage.service;

import com.seyoung5309.executorserver.domain.friend.repository.FriendRepository;
import com.seyoung5309.executorserver.domain.stage.dto.RankingResponse;
import com.seyoung5309.executorserver.domain.stage.entity.Stage;
import com.seyoung5309.executorserver.domain.stage.entity.UserStage;
import com.seyoung5309.executorserver.domain.stage.repository.RankingRow;
import com.seyoung5309.executorserver.domain.stage.repository.UserStageRepository;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import com.seyoung5309.executorserver.global.response.UserSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static com.seyoung5309.executorserver.domain.stage.StageFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class StageRankingServiceTest {

    private StageAccessService accessService;
    private UserStageRepository userStageRepository;
    private FriendRepository friendRepository;
    private StageRankingService service;

    private final Stage stage = officialStage(10L, operation(1L, "op", 1), 1);

    @BeforeEach
    void setUp() {
        accessService = mock(StageAccessService.class);
        userStageRepository = mock(UserStageRepository.class);
        friendRepository = mock(FriendRepository.class);
        service = new StageRankingService(accessService, userStageRepository, friendRepository);
        when(accessService.getPlayableStage(1L, 10L)).thenReturn(stage);
    }

    @Test
    void 전체_순위는_조회된_순서대로_1부터_번호를_붙인다() {
        when(userStageRepository.findRanking(eq(10L), any())).thenReturn(List.of(
                new RankingRow(12L, "nullptr", 5L, 15),
                new RankingRow(1L, "debugger", null, 18)));
        when(userStageRepository.findByUserIdAndStageId(1L, 10L)).thenReturn(Optional.of(cleared(stage, 18, false)));
        when(userStageRepository.countAhead(eq(10L), eq(18), any(Instant.class), eq(1L))).thenReturn(1L);

        RankingResponse response = service.getRanking(1L, 10L, RankingScope.ALL, 20);

        assertThat(response.rankings()).containsExactly(
                new RankingResponse.Entry(1, new UserSummary(12L, "nullptr", 5L), 15),
                new RankingResponse.Entry(2, new UserSummary(1L, "debugger", null), 18));
        assertThat(response.me()).isEqualTo(new RankingResponse.MyRank(2, 18));
    }

    @Test
    void size만큼만_조회한다() {
        when(userStageRepository.findRanking(eq(10L), any())).thenReturn(List.of());

        service.getRanking(1L, 10L, RankingScope.ALL, 5);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(userStageRepository).findRanking(eq(10L), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(5);
        assertThat(captor.getValue().getPageNumber()).isZero();
    }

    @Test
    void 클리어하지_않았으면_me는_null() {
        when(userStageRepository.findRanking(eq(10L), any())).thenReturn(List.of());
        when(userStageRepository.findByUserIdAndStageId(1L, 10L)).thenReturn(Optional.of(notCleared(stage, 4)));

        assertThat(service.getRanking(1L, 10L, RankingScope.ALL, 20).me()).isNull();
        verify(userStageRepository, never()).countAhead(anyLong(), anyInt(), any(), anyLong());
    }

    @SuppressWarnings("unchecked")
    @Test
    void 친구_순위는_나와_친구만_대상으로_조회하고_내_순위도_그_안에서_센다() {
        when(friendRepository.findFriendIds(1L)).thenReturn(List.of(12L, 30L));
        when(userStageRepository.findRankingAmong(eq(10L), anyCollection(), any())).thenReturn(List.of());
        UserStage mine = cleared(stage, 18, false);
        when(userStageRepository.findByUserIdAndStageId(1L, 10L)).thenReturn(Optional.of(mine));
        when(userStageRepository.countAheadAmong(eq(10L), anyCollection(), eq(18), any(Instant.class), eq(1L))).thenReturn(0L);

        RankingResponse response = service.getRanking(1L, 10L, RankingScope.FRIENDS, 20);

        ArgumentCaptor<Collection<Long>> members = ArgumentCaptor.forClass(Collection.class);
        verify(userStageRepository).findRankingAmong(eq(10L), members.capture(), any());
        assertThat(members.getValue()).containsExactlyInAnyOrder(1L, 12L, 30L);
        assertThat(response.me()).isEqualTo(new RankingResponse.MyRank(1, 18));
        verify(userStageRepository, never()).findRanking(anyLong(), any());
    }

    @Test
    void 친구가_없어도_나_혼자_순위에_들어간다() {
        when(friendRepository.findFriendIds(1L)).thenReturn(List.of());
        when(userStageRepository.findRankingAmong(eq(10L), anyCollection(), any())).thenReturn(List.of());

        service.getRanking(1L, 10L, RankingScope.FRIENDS, 20);

        verify(userStageRepository).findRankingAmong(eq(10L), argThat(ids -> ids.size() == 1 && ids.contains(1L)), any());
    }

    @Test
    void 볼_수_없는_스테이지면_예외를_그대로_전달한다() {
        when(accessService.getPlayableStage(1L, 11L)).thenThrow(new BusinessException(ErrorCode.STAGE_LOCKED));

        assertThatThrownBy(() -> service.getRanking(1L, 11L, RankingScope.ALL, 20))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.STAGE_LOCKED);
    }

    @Test
    void scope는_대소문자를_가리지_않고_잘못된_값은_INVALID_INPUT() {
        assertThat(RankingScope.parse("Friends")).isEqualTo(RankingScope.FRIENDS);
        assertThat(RankingScope.parse(" all ")).isEqualTo(RankingScope.ALL);
        assertThatThrownBy(() -> RankingScope.parse("world"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }
}
