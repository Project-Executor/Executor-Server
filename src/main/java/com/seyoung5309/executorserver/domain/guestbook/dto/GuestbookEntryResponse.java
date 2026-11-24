package com.seyoung5309.executorserver.domain.guestbook.dto;

import com.seyoung5309.executorserver.domain.guestbook.entity.GuestbookEntry;

import java.time.Instant;

/** 방명록 글 (이름·내용은 가린 뒤의 값) */
public record GuestbookEntryResponse(Long id, String name, String message, Instant createdAt) {

    public static GuestbookEntryResponse from(GuestbookEntry e) {
        return new GuestbookEntryResponse(e.getId(), e.getName(), e.getMessage(), e.getCreatedAt());
    }
}
