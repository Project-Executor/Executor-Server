package com.seyoung5309.executorserver.domain.message.controller;

import com.seyoung5309.executorserver.domain.message.dto.*;
import com.seyoung5309.executorserver.domain.message.service.MessageService;
import com.seyoung5309.executorserver.global.response.PageResponse;
import com.seyoung5309.executorserver.global.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "메시지", description = "친구와 주고받는 메시지")
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @Operation(summary = "메시지 보내기", description = "친구에게만 보낼 수 있다(아니면 409 NOT_FRIEND). content는 1~500자.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SendMessageResponse send(@LoginUser Long userId, @Valid @RequestBody SendMessageRequest request) {
        return messageService.send(userId, request.receiverId(), request.content());
    }

    @Operation(summary = "대화 상대 목록", description = "마지막 메시지가 최근인 순. 상대별 마지막 메시지와 안 읽은 수.")
    @GetMapping("/rooms")
    public List<MessageRoomResponse> getRooms(@LoginUser Long userId) {
        return messageService.getRooms(userId);
    }

    @Operation(summary = "안 읽은 메시지 수", description = "나에게 온 메시지 전체 중 안 읽은 수.")
    @GetMapping("/unread-count")
    public UnreadCountResponse getUnreadCount(@LoginUser Long userId) {
        return messageService.getUnreadCount(userId);
    }

    @Operation(summary = "대화 내용",
            description = "최신순, 페이지 단위(size 1~50, 기본 30). 조회하면 상대가 보낸 메시지는 읽음 처리된다.")
    @GetMapping("/{partnerId}")
    public PageResponse<MessageResponse> getConversation(
            @LoginUser Long userId,
            @PathVariable Long partnerId,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page는 0 이상이어야 합니다.") int page,
            @RequestParam(defaultValue = "30")
            @Min(value = 1, message = "size는 1 이상이어야 합니다.")
            @Max(value = 50, message = "size는 50 이하여야 합니다.")
            int size) {
        return messageService.getConversation(userId, partnerId, page, size);
    }
}
