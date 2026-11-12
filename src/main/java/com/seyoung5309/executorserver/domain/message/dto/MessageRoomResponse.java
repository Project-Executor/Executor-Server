package com.seyoung5309.executorserver.domain.message.dto;

import com.seyoung5309.executorserver.global.response.UserSummary;

import java.time.Instant;

/**
 * 대화 상대 한 명.
 *
 * @param unreadCount 상대가 나에게 보낸 메시지 중 안 읽은 수
 */
public record MessageRoomResponse(UserSummary partner, String lastMessage, Instant lastMessageAt, long unreadCount) {
}
