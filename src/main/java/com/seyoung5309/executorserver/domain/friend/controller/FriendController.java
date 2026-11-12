package com.seyoung5309.executorserver.domain.friend.controller;

import com.seyoung5309.executorserver.domain.friend.dto.FriendRequestCreateRequest;
import com.seyoung5309.executorserver.domain.friend.dto.FriendRequestItemResponse;
import com.seyoung5309.executorserver.domain.friend.dto.FriendRequestStatusResponse;
import com.seyoung5309.executorserver.domain.friend.dto.FriendResponse;
import com.seyoung5309.executorserver.domain.friend.service.FriendService;
import com.seyoung5309.executorserver.global.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "친구", description = "친구 목록, 친구 요청")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    @Operation(summary = "친구 목록", description = "닉네임 순. since는 친구가 된 시각.")
    @GetMapping("/friends")
    public List<FriendResponse> getFriends(@LoginUser Long userId) {
        return friendService.getFriends(userId);
    }

    @Operation(summary = "친구 삭제",
            description = "친구가 아니면 404. 두 사람 사이의 요청 기록도 지워지므로 나중에 다시 요청할 수 있다.")
    @DeleteMapping("/friends/{friendUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFriend(@LoginUser Long userId, @PathVariable Long friendUserId) {
        friendService.deleteFriend(userId, friendUserId);
    }

    @Operation(summary = "친구 요청 보내기",
            description = "상대가 나에게 보낸 대기 중 요청이 있으면 바로 친구가 된다(status = ACCEPTED). "
                    + "거절된 요청은 다시 보낼 수 있다(같은 요청이 PENDING으로 돌아감).")
    @PostMapping("/friend-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public FriendRequestStatusResponse send(@LoginUser Long userId,
                                            @Valid @RequestBody FriendRequestCreateRequest request) {
        return friendService.send(userId, request.receiverId());
    }

    @Operation(summary = "받은 친구 요청", description = "대기 중인 요청만, 최근 순. user는 보낸 사람.")
    @GetMapping("/friend-requests/received")
    public List<FriendRequestItemResponse> getReceived(@LoginUser Long userId) {
        return friendService.getReceived(userId);
    }

    @Operation(summary = "보낸 친구 요청", description = "대기 중인 요청만, 최근 순. user는 받는 사람.")
    @GetMapping("/friend-requests/sent")
    public List<FriendRequestItemResponse> getSent(@LoginUser Long userId) {
        return friendService.getSent(userId);
    }

    @Operation(summary = "친구 요청 수락", description = "나에게 온 대기 중 요청만. 수락하면 바로 친구가 된다.")
    @PostMapping("/friend-requests/{requestId}/accept")
    public FriendRequestStatusResponse accept(@LoginUser Long userId, @PathVariable Long requestId) {
        return friendService.accept(userId, requestId);
    }

    @Operation(summary = "친구 요청 거절", description = "나에게 온 대기 중 요청만. 상대는 나중에 다시 요청할 수 있다.")
    @PostMapping("/friend-requests/{requestId}/reject")
    public FriendRequestStatusResponse reject(@LoginUser Long userId, @PathVariable Long requestId) {
        return friendService.reject(userId, requestId);
    }

    @Operation(summary = "보낸 친구 요청 취소", description = "내가 보낸 대기 중 요청만.")
    @DeleteMapping("/friend-requests/{requestId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@LoginUser Long userId, @PathVariable Long requestId) {
        friendService.cancel(userId, requestId);
    }
}
