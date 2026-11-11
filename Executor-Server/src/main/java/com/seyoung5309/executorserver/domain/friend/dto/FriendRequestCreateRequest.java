package com.seyoung5309.executorserver.domain.friend.dto;

import jakarta.validation.constraints.NotNull;

public record FriendRequestCreateRequest(
        @NotNull(message = "receiverId를 입력해 주세요.")
        Long receiverId
) {
}
