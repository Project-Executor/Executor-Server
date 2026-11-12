package com.seyoung5309.executorserver.domain.friend.dto;

import com.seyoung5309.executorserver.global.response.UserSummary;

import java.time.Instant;

/**
 * 대기 중인 요청 하나.
 *
 * @param user 받은 요청 목록에서는 보낸 사람, 보낸 요청 목록에서는 받는 사람
 */
public record FriendRequestItemResponse(Long requestId, UserSummary user, Instant createdAt) {
}
