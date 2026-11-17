package com.seyoung5309.executorserver.domain.notice.service;

import com.seyoung5309.executorserver.domain.notice.entity.Notice;
import com.seyoung5309.executorserver.domain.notice.entity.NoticeCategory;
import com.seyoung5309.executorserver.domain.notice.repository.NoticeRepository;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class NoticeServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-17T03:00:00Z");

    private NoticeRepository noticeRepository;
    private NoticeService service;

    @BeforeEach
    void setUp() {
        noticeRepository = mock(NoticeRepository.class);
        service = new NoticeService(noticeRepository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void 목록은_지금_시각으로_보이는_공지를_찾는다() {
        when(noticeRepository.findShown(eq(NOW), any())).thenReturn(new PageImpl<>(List.of(notice(1L, true, NOW, null))));

        var page = service.getNotices(0, 20);

        assertThat(page.content()).singleElement().satisfies(n -> {
            assertThat(n.noticeId()).isEqualTo(1L);
            assertThat(n.title()).isEqualTo("제목");
        });
    }

    @Test
    void 보이는_공지는_상세를_돌려준다() {
        when(noticeRepository.findById(1L)).thenReturn(Optional.of(notice(1L, true, NOW.minusSeconds(1), null)));

        assertThat(service.getNotice(1L).content()).isEqualTo("내용");
    }

    @Test
    void 숨김_예약_종료_없는_공지는_NOT_FOUND() {
        when(noticeRepository.findById(1L)).thenReturn(Optional.of(notice(1L, false, NOW.minusSeconds(60), null)));
        when(noticeRepository.findById(2L)).thenReturn(Optional.of(notice(2L, true, NOW.plusSeconds(60), null)));
        when(noticeRepository.findById(3L)).thenReturn(Optional.of(notice(3L, true, NOW.minus(Duration.ofDays(2)), NOW)));
        when(noticeRepository.findById(4L)).thenReturn(Optional.empty());

        for (long id = 1; id <= 4; id++) {
            long noticeId = id;
            assertThatThrownBy(() -> service.getNotice(noticeId))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }
    }

    @Test
    void 게시_시각과_종료_시각_경계() {
        // 게시 시각 = 지금이면 보이고, 종료 시각 = 지금이면 이미 끝난 것
        assertThat(notice(1L, true, NOW, null).isShownAt(NOW)).isTrue();
        assertThat(notice(1L, true, NOW.minusSeconds(60), NOW).isShownAt(NOW)).isFalse();
        assertThat(notice(1L, true, NOW.minusSeconds(60), NOW.plusSeconds(1)).isShownAt(NOW)).isTrue();
    }

    static Notice notice(Long id, boolean visible, Instant publishedAt, Instant endsAt) {
        Notice notice = new Notice(NoticeCategory.NOTICE, "제목", "내용", false, visible, publishedAt, endsAt);
        ReflectionTestUtils.setField(notice, "id", id);
        return notice;
    }
}
