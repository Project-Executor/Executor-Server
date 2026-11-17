package com.seyoung5309.executorserver.domain.notice.dto;

import com.seyoung5309.executorserver.domain.notice.entity.Notice;
import com.seyoung5309.executorserver.domain.notice.entity.NoticeCategory;

import java.time.Instant;

/** 게임 공지 상세 */
public record NoticeDetailResponse(
        Long noticeId,
        NoticeCategory category,
        String title,
        String content,
        boolean isPinned,
        Instant publishedAt,
        Instant updatedAt
) {
    public static NoticeDetailResponse from(Notice notice) {
        return new NoticeDetailResponse(notice.getId(), notice.getCategory(), notice.getTitle(), notice.getContent(),
                notice.isPinned(), notice.getPublishedAt(), notice.getUpdatedAt());
    }
}
