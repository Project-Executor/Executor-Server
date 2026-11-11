package com.seyoung5309.executorserver.domain.message.dto;

import java.time.Instant;

public record SendMessageResponse(Long messageId, Instant createdAt) {
}
