package com.seyoung5309.executorserver.domain.guestbook.dto;

import java.util.List;

/**
 * 최신 방명록 (메인 위젯·채팅 창).
 *
 * @param entries 최신 limit개를 오래된 것부터 (채팅처럼 아래로 쌓이는 순서)
 * @param today   오늘(한국 시간) 남긴 글 수
 * @param total   전체 글 수
 */
public record GuestbookLatestResponse(List<GuestbookEntryResponse> entries, long today, long total) {
}
