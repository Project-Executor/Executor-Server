package com.seyoung5309.executorserver.domain.friend.dto;

import com.seyoung5309.executorserver.domain.friend.entity.FriendRequest;
import com.seyoung5309.executorserver.domain.friend.entity.FriendRequestStatus;

/** 요청을 보내거나 수락·거절한 결과. 보냈는데 바로 친구가 됐으면 status = ACCEPTED */
public record FriendRequestStatusResponse(Long requestId, FriendRequestStatus status) {

    public static FriendRequestStatusResponse from(FriendRequest request) {
        return new FriendRequestStatusResponse(request.getId(), request.getStatus());
    }
}
