package com.seyoung5309.executorserver.domain.guestbook.service;

import com.seyoung5309.executorserver.domain.guestbook.dto.GuestbookRequest;
import com.seyoung5309.executorserver.domain.guestbook.entity.GuestbookEntry;
import com.seyoung5309.executorserver.domain.guestbook.repository.GuestbookRepository;
import com.seyoung5309.executorserver.domain.profile.repository.UserRepository;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class GuestbookServiceTest {

    /** 한국 시간 2026-11-25 12:00 (UTC 03:00). 오늘 시작 = UTC 2026-11-24 15:00 */
    private static final Instant NOW = Instant.parse("2026-11-25T03:00:00Z");
    private static final Instant TODAY = Instant.parse("2026-11-24T15:00:00Z");

    private GuestbookRepository repository;
    private UserRepository userRepository;
    private GuestbookService service;

    @BeforeEach
    void setUp() {
        repository = mock(GuestbookRepository.class);
        userRepository = mock(UserRepository.class);
        service = new GuestbookService(repository, userRepository, Clock.fixed(NOW, ZoneOffset.UTC));
        when(repository.save(any())).thenAnswer(inv -> {
            GuestbookEntry e = inv.getArgument(0);
            ReflectionTestUtils.setField(e, "id", 7L);
            return e;
        });
    }

    private static void assertErrorCode(Runnable call, ErrorCode code) {
        assertThatThrownBy(call::run).isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(code);
    }

    @Test
    void 게스트는_보상_없이_남기고_공백은_정리된다() {
        var res = service.post(null, new GuestbookRequest("  이밤 ", " 와 주셔서\n감사합니다 ", "pc-1"));

        assertThat(res.entry().name()).isEqualTo("이밤");
        assertThat(res.entry().message()).isEqualTo("와 주셔서 감사합니다");
        assertThat(res.entry().createdAt()).isEqualTo(NOW);
        assertThat(res.rewardCoin()).isZero();
        assertThat(res.totalCoin()).isNull();
        verify(userRepository, never()).addCoin(anyLong(), anyInt());
    }

    @Test
    void 로그인_사용자의_그날_첫_글은_코인_10개() {
        when(repository.existsByUserIdAndCreatedAtGreaterThanEqual(3L, TODAY)).thenReturn(false);
        when(userRepository.addCoin(3L, 10)).thenReturn(1);
        when(userRepository.findCoinByUserId(3L)).thenReturn(Optional.of(190));

        var res = service.post(3L, new GuestbookRequest("이밤", "안녕", null));

        assertThat(res.rewardCoin()).isEqualTo(10);
        assertThat(res.totalCoin()).isEqualTo(190);
    }

    @Test
    void 오늘_이미_남겼으면_보상_없음() {
        when(repository.existsByUserIdAndCreatedAtGreaterThanEqual(3L, TODAY)).thenReturn(true);

        var res = service.post(3L, new GuestbookRequest("이밤", "또 왔어요", null));

        assertThat(res.rewardCoin()).isZero();
        verify(userRepository, never()).addCoin(anyLong(), anyInt());
    }

    @Test
    void 같은_기기는_5초에_한_번() {
        when(repository.existsByDeviceIdAndCreatedAtGreaterThanEqual("pc-1", NOW.minusSeconds(5))).thenReturn(true);
        assertErrorCode(() -> service.post(null, new GuestbookRequest("이밤", "안녕", "pc-1")), ErrorCode.MESSAGE_SPAM);
        verify(repository, never()).save(any());
    }

    @Test
    void 길이_검사() {
        assertErrorCode(() -> service.post(null, new GuestbookRequest("   ", "안녕", null)), ErrorCode.INVALID_INPUT);
        assertErrorCode(() -> service.post(null, new GuestbookRequest("열한글자가넘는이름입니", "안녕", null)), ErrorCode.INVALID_INPUT);
        assertErrorCode(() -> service.post(null, new GuestbookRequest("이밤", "가".repeat(101), null)), ErrorCode.INVALID_INPUT);
        assertThat(service.post(null, new GuestbookRequest("열글자이름입니다요", "가".repeat(100), null)).entry().message()).hasSize(100);
    }

    @Test
    void 욕설과_개인정보는_가린다() {
        assertThat(GuestbookFilter.mask("연락 010-1234-5678 주세요")).isEqualTo("연락 ************* 주세요");
        assertThat(GuestbookFilter.mask("메일 me@test.com")).isEqualTo("메일 ***********");
        assertThat(GuestbookFilter.mask("시 발 진짜")).isEqualTo("*** 진짜");
        assertThat(GuestbookFilter.mask("FUCK")).isEqualTo("****");
        assertThat(GuestbookFilter.mask("시바견 귀여워")).isEqualTo("시바견 귀여워");

        var res = service.post(null, new GuestbookRequest("이밤", "병신같은 버그 010 1111 2222", null));
        assertThat(res.entry().message()).isEqualTo("**같은 버그 *************");
    }

    @Test
    void 최신글은_오래된_것부터_오늘_전체_수와_함께() {
        GuestbookEntry newer = new GuestbookEntry("b", "2", null, null, NOW);
        GuestbookEntry older = new GuestbookEntry("a", "1", null, null, NOW.minusSeconds(60));
        when(repository.findLatest(any(Pageable.class))).thenReturn(List.of(newer, older));
        when(repository.countByCreatedAtGreaterThanEqual(TODAY)).thenReturn(42L);
        when(repository.count()).thenReturn(128L);

        var res = service.latest(30);

        assertThat(res.entries()).extracting(e -> e.name()).containsExactly("a", "b");
        assertThat(res.today()).isEqualTo(42);
        assertThat(res.total()).isEqualTo(128);
    }

    @Test
    void 관리자_삭제_없는_글은_NOT_FOUND() {
        when(repository.findById(eq(9L))).thenReturn(Optional.empty());
        assertErrorCode(() -> service.delete(9L), ErrorCode.NOT_FOUND);
    }
}
