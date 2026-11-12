package com.seyoung5309.executorserver.domain.friend.dto;

import com.seyoung5309.executorserver.global.response.UserSummary;

import java.time.Instant;

/**
 * 친구 한 명.
 *
 * @param since 친구가 된 시각
 */
public record FriendResponse(UserSummary user, Instant since) {
}
