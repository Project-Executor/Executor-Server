package com.seyoung5309.executorserver.domain.admin.dto;

import com.seyoung5309.executorserver.domain.notice.entity.Notice;
import com.seyoung5309.executorserver.domain.notice.entity.NoticeCategory;

import java.time.Instant;

/**
 * 관리자 공지 (숨긴·예약·끝난 공지 포함).
 *
 * @param isShown 지금 게임에 보이는지 (숨기지 않았고 게시 기간 안)
 */
public record AdminNoticeResponse(
        Long noticeId,
        NoticeCategory category,
        String title,
        String content,
        boolean isPinned,
        boolean isVisible,
        Instant publishedAt,
        Instant endsAt,
        boolean isShown,
        Instant createdAt,
        Instant updatedAt
) {
    public static AdminNoticeResponse of(Notice notice, Instant now) {
        return new AdminNoticeResponse(notice.getId(), notice.getCategory(), notice.getTitle(), notice.getContent(),
                notice.isPinned(), notice.isVisible(), notice.getPublishedAt(), notice.getEndsAt(),
                notice.isShownAt(now), notice.getCreatedAt(), notice.getUpdatedAt());
    }
}
