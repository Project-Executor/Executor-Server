package com.seyoung5309.executorserver.domain.event.service;

import com.seyoung5309.executorserver.domain.event.dto.EventRecordRequest;
import com.seyoung5309.executorserver.domain.event.entity.EventRecord;
import com.seyoung5309.executorserver.domain.event.repository.EventRecordRepository;
import com.seyoung5309.executorserver.domain.stage.entity.Operation;
import com.seyoung5309.executorserver.domain.stage.entity.Stage;
import com.seyoung5309.executorserver.domain.stage.entity.StageType;
import com.seyoung5309.executorserver.domain.stage.repository.StageRepository;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static com.seyoung5309.executorserver.domain.stage.StageFixtures.officialStage;
import static com.seyoung5309.executorserver.domain.stage.StageFixtures.operation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class EventRecordServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-25T03:00:00Z");

    private EventRecordRepository recordRepository;
    private StageRepository stageRepository;
    private EventRecordService service;
    private Stage turns, typing, normal;

    @BeforeEach
    void setUp() {
        recordRepository = mock(EventRecordRepository.class);
        stageRepository = mock(StageRepository.class);
        service = new EventRecordService(recordRepository, stageRepository, Clock.fixed(NOW, ZoneOffset.UTC));
        Operation op = operation(1L, "전시", 0);
        turns = officialStage(10L, op, 1, StageType.EVENT_TURNS, 20);
        typing = officialStage(11L, op, 2, StageType.EVENT_TYPING, 1);
        normal = officialStage(12L, op, 3);
        when(stageRepository.findById(10L)).thenReturn(Optional.of(turns));
        when(stageRepository.findById(11L)).thenReturn(Optional.of(typing));
        when(stageRepository.findById(12L)).thenReturn(Optional.of(normal));
        when(recordRepository.saveAndFlush(any())).thenAnswer(inv -> {
            EventRecord r = inv.getArgument(0);
            ReflectionTestUtils.setField(r, "id", 100L);
            return r;
        });
    }

    private static void assertErrorCode(Runnable call, ErrorCode code) {
        assertThatThrownBy(call::run).isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(code);
    }

    private static EventRecord record(long id, String name, int score) {
        EventRecord r = new EventRecord(10L, name, score, NOW);
        ReflectionTestUtils.setField(r, "id", id);
        return r;
    }

    @Test
    void 턴수_이벤트는_적을수록_위_같은_점수는_같은_순위() {
        when(recordRepository.findTopAscending(eq(10L), any(Pageable.class)))
                .thenReturn(List.of(record(1, "가", 12), record(2, "나", 12), record(3, "다", 15)));
        when(recordRepository.countByStageId(10L)).thenReturn(3L);

        var res = service.getRanking(10L, 20);

        assertThat(res.stageType()).isEqualTo(StageType.EVENT_TURNS);
        assertThat(res.rankings()).extracting(e -> e.rank()).containsExactly(1, 1, 3);
        assertThat(res.total()).isEqualTo(3);
        verify(recordRepository, never()).findTopDescending(any(), any());
    }

    @Test
    void 타자_이벤트는_많을수록_위() {
        when(recordRepository.findTopDescending(eq(11L), any(Pageable.class))).thenReturn(List.of());
        service.getRanking(11L, 20);
        verify(recordRepository).findTopDescending(eq(11L), any(Pageable.class));
    }

    @Test
    void 올리면_이름_공백을_지우고_순위를_돌려준다() {
        when(recordRepository.countBetterAscending(10L, 14)).thenReturn(2L);
        when(recordRepository.countByStageId(10L)).thenReturn(5L);

        var res = service.submit(10L, new EventRecordRequest("  민수 ", 14));

        assertThat(res.name()).isEqualTo("민수");
        assertThat(res.rank()).isEqualTo(3);
        assertThat(res.total()).isEqualTo(5);
        assertThat(res.recordId()).isEqualTo(100L);
    }

    @Test
    void 같은_이름은_DUPLICATE_VALUE() {
        when(recordRepository.existsByStageIdAndName(10L, "민수")).thenReturn(true);
        assertErrorCode(() -> service.submit(10L, new EventRecordRequest("민수", 14)), ErrorCode.DUPLICATE_VALUE);
        verify(recordRepository, never()).saveAndFlush(any());
    }

    @Test
    void 동시에_같은_이름이_들어와_UNIQUE에_걸려도_DUPLICATE_VALUE() {
        doThrow(new DataIntegrityViolationException("uk")).when(recordRepository).saveAndFlush(any());
        assertErrorCode(() -> service.submit(10L, new EventRecordRequest("민수", 14)), ErrorCode.DUPLICATE_VALUE);
    }

    @Test
    void 이름_길이_제어문자_점수_범위() {
        assertErrorCode(() -> service.submit(10L, new EventRecordRequest("   ", 14)), ErrorCode.INVALID_INPUT);
        assertErrorCode(() -> service.submit(10L, new EventRecordRequest("열세글자가넘는아주긴이름입니다", 14)), ErrorCode.INVALID_INPUT);
        assertErrorCode(() -> service.submit(10L, new EventRecordRequest("a\nb", 14)), ErrorCode.INVALID_INPUT);
        assertErrorCode(() -> service.submit(10L, new EventRecordRequest("민수", 0)), ErrorCode.INVALID_INPUT);
        assertErrorCode(() -> service.submit(11L, new EventRecordRequest("민수", 1001)), ErrorCode.INVALID_INPUT);
        // 타자 이벤트는 0개도 기록할 수 있다
        when(recordRepository.countByStageId(11L)).thenReturn(1L);
        assertThat(service.submit(11L, new EventRecordRequest("민수", 0)).rank()).isEqualTo(1);
    }

    @Test
    void 이벤트가_아닌_스테이지는_NOT_FOUND() {
        assertErrorCode(() -> service.getRanking(12L, 20), ErrorCode.NOT_FOUND);
        assertErrorCode(() -> service.submit(12L, new EventRecordRequest("민수", 3)), ErrorCode.NOT_FOUND);
        assertErrorCode(() -> service.getRanking(999L, 20), ErrorCode.NOT_FOUND);
    }

    @Test
    void 관리자_삭제는_다른_스테이지_기록이면_NOT_FOUND() {
        EventRecord other = new EventRecord(11L, "x", 1, NOW);
        when(recordRepository.findById(5L)).thenReturn(Optional.of(other));
        assertErrorCode(() -> service.delete(10L, 5L), ErrorCode.NOT_FOUND);

        when(recordRepository.deleteAllByStage(10L)).thenReturn(4);
        assertThat(service.deleteAll(10L)).isEqualTo(4);
    }
}
