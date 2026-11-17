package com.seyoung5309.executorserver.domain.notice.dto;

import com.seyoung5309.executorserver.domain.notice.entity.Notice;
import com.seyoung5309.executorserver.domain.notice.entity.NoticeCategory;

import java.time.Instant;

/** 게임 공지 목록 항목 (내용 제외) */
public record NoticeSummaryResponse(
        Long noticeId,
        NoticeCategory category,
        String title,
        boolean isPinned,
        Instant publishedAt
) {
    public static NoticeSummaryResponse from(Notice notice) {
        return new NoticeSummaryResponse(notice.getId(), notice.getCategory(), notice.getTitle(),
                notice.isPinned(), notice.getPublishedAt());
    }
}
