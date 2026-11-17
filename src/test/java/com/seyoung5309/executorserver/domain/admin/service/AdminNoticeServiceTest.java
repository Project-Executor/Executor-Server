package com.seyoung5309.executorserver.domain.admin.service;

import com.seyoung5309.executorserver.domain.admin.dto.AdminNoticeRequest;
import com.seyoung5309.executorserver.domain.notice.entity.Notice;
import com.seyoung5309.executorserver.domain.notice.entity.NoticeCategory;
import com.seyoung5309.executorserver.domain.notice.repository.NoticeRepository;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminNoticeServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-17T03:00:00Z");

    private NoticeRepository noticeRepository;
    private AdminNoticeService service;

    @BeforeEach
    void setUp() {
        noticeRepository = mock(NoticeRepository.class);
        service = new AdminNoticeService(noticeRepository, Clock.fixed(NOW, ZoneOffset.UTC));
        when(noticeRepository.save(any(Notice.class))).thenAnswer(inv -> {
            Notice n = inv.getArgument(0);
            ReflectionTestUtils.setField(n, "id", 1L);
            return n;
        });
    }

    @Test
    void 생략한_값은_지금_게시_고정_안함_보임으로_등록한다() {
        var res = service.create(request(" 점검 안내 ", "  내용  ", null, null, null, null));

        ArgumentCaptor<Notice> saved = ArgumentCaptor.forClass(Notice.class);
        verify(noticeRepository).save(saved.capture());
        Notice n = saved.getValue();
        assertThat(n.getTitle()).isEqualTo("점검 안내");
        assertThat(n.getContent()).isEqualTo("내용");
        assertThat(n.getPublishedAt()).isEqualTo(NOW);
        assertThat(n.isPinned()).isFalse();
        assertThat(n.isVisible()).isTrue();
        assertThat(res.isShown()).isTrue();
    }

    @Test
    void 미래_게시는_예약이라_아직_보이지_않는다() {
        Instant later = Instant.parse("2026-11-18T00:00:00.789Z");

        var res = service.create(request("t", "c", true, false, later, null));

        assertThat(res.publishedAt()).isEqualTo(Instant.parse("2026-11-18T00:00:00Z")); // 초 단위로 맞춤
        assertThat(res.isPinned()).isTrue();
        assertThat(res.isVisible()).isFalse();
        assertThat(res.isShown()).isFalse();
    }

    @Test
    void 종료가_게시보다_앞이거나_같으면_INVALID_INPUT() {
        assertErrorCode(() -> service.create(request("t", "c", null, null, NOW, NOW)), ErrorCode.INVALID_INPUT);
        assertErrorCode(() -> service.create(request("t", "c", null, null, null, NOW.minusSeconds(1))),
                ErrorCode.INVALID_INPUT);
        verify(noticeRepository, never()).save(any());
    }

    @Test
    void 수정에서_publishedAt을_생략하면_기존_게시_시각을_유지한다() {
        Instant original = Instant.parse("2026-11-01T00:00:00Z");
        Notice notice = new Notice(NoticeCategory.NOTICE, "old", "old", false, true, original, null);
        ReflectionTestUtils.setField(notice, "id", 5L);
        when(noticeRepository.findById(5L)).thenReturn(Optional.of(notice));

        var res = service.update(5L, new AdminNoticeRequest(NoticeCategory.EVENT, "new", "new", true, true, null, null));

        assertThat(res.publishedAt()).isEqualTo(original);
        assertThat(notice.getCategory()).isEqualTo(NoticeCategory.EVENT);
        assertThat(notice.getTitle()).isEqualTo("new");
        assertThat(notice.isPinned()).isTrue();
    }

    @Test
    void 없는_공지는_NOT_FOUND() {
        when(noticeRepository.findById(9L)).thenReturn(Optional.empty());
        assertErrorCode(() -> service.get(9L), ErrorCode.NOT_FOUND);
        assertErrorCode(() -> service.delete(9L), ErrorCode.NOT_FOUND);
        assertErrorCode(() -> service.update(9L, request("t", "c", null, null, null, null)), ErrorCode.NOT_FOUND);
    }

    private static AdminNoticeRequest request(String title, String content, Boolean pinned, Boolean visible,
                                              Instant publishedAt, Instant endsAt) {
        return new AdminNoticeRequest(NoticeCategory.NOTICE, title, content, pinned, visible, publishedAt, endsAt);
    }

    private static void assertErrorCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, ErrorCode code) {
        assertThatThrownBy(call).isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(code));
    }
}
