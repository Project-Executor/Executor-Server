package com.seyoung5309.executorserver.domain.friend.repository;

import java.time.Instant;

/** 친구 한 명: 상대 사용자 ID와 친구가 된 시각 */
public record FriendRow(Long friendUserId, Instant since) {
}
