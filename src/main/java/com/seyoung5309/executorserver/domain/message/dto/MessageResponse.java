package com.seyoung5309.executorserver.domain.message.dto;

import com.seyoung5309.executorserver.domain.message.entity.Message;

import java.time.Instant;

/**
 * 대화 속 메시지 한 개.
 *
 * @param isRead 받는 사람이 읽었는지. 내가 보낸 메시지면 상대가 읽었는지를 뜻한다
 */
public record MessageResponse(Long messageId, Long senderId, String content, boolean isRead, Instant createdAt) {

    public static MessageResponse from(Message message) {
        return new MessageResponse(message.getId(), message.getSender().getId(), message.getContent(),
                message.isRead(), message.getCreatedAt());
    }
}
