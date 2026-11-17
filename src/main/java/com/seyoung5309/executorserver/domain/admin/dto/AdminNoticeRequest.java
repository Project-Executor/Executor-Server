package com.seyoung5309.executorserver.domain.admin.dto;

import com.seyoung5309.executorserver.domain.notice.entity.NoticeCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * 공지 등록·수정 (수정할 때도 모든 값을 보낸다).
 *
 * @param isPinned    상단 고정 (생략하면 false)
 * @param isVisible   false면 숨김 (생략하면 true)
 * @param publishedAt 게시 시각 (ISO-8601 UTC). 생략하면 등록은 지금, 수정은 기존 값. 미래면 예약 게시
 * @param endsAt      게시 종료 시각. null이면 계속. 값이 있으면 publishedAt보다 뒤여야 한다
 */
public record AdminNoticeRequest(
        @NotNull(message = "category를 입력해 주세요. (NOTICE, UPDATE, EVENT, MAINTENANCE)")
        NoticeCategory category,

        @NotBlank(message = "title을 입력해 주세요.")
        @Size(max = 100, message = "title은 100자 이하여야 합니다.")
        String title,

        @NotBlank(message = "content를 입력해 주세요.")
        @Size(max = 5000, message = "content는 5000자 이하여야 합니다.")
        String content,

        Boolean isPinned,

        Boolean isVisible,

        Instant publishedAt,

        Instant endsAt
) {
}
