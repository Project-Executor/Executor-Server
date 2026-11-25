package com.seyoung5309.executorserver.domain.guestbook.controller;

import com.seyoung5309.executorserver.domain.guestbook.dto.GuestbookLatestResponse;
import com.seyoung5309.executorserver.domain.guestbook.dto.GuestbookPostResponse;
import com.seyoung5309.executorserver.domain.guestbook.dto.GuestbookRequest;
import com.seyoung5309.executorserver.domain.guestbook.service.GuestbookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "방명록", description = "전시용. 로그인 없이 보고 남긴다 (로그인하고 남기면 하루 첫 글에 코인 10개)")
@Validated
@RestController
@RequestMapping("/api/guestbook")
@RequiredArgsConstructor
public class GuestbookController {

    private final GuestbookService guestbookService;

    @Operation(summary = "최신 방명록",
            description = "최신 limit개(1~50, 기본 30)를 오래된 것부터, 오늘(한국 시간)·전체 글 수와 함께. 메인 위젯·채팅 창이 몇 초마다 다시 부른다.")
    @GetMapping("/latest")
    public GuestbookLatestResponse latest(@RequestParam(defaultValue = "30")
                                          @Min(value = 1, message = "limit은 1 이상이어야 합니다.")
                                          @Max(value = 50, message = "limit은 50 이하여야 합니다.")
                                          int limit) {
        return guestbookService.latest(limit);
    }

    @Operation(summary = "방명록 남기기",
            description = "이름 1~10자, 내용 1~100자 (앞뒤 공백 제외). 욕설·전화번호·이메일은 * 로 가려서 저장한다. "
                    + "같은 deviceId는 5초에 한 번(429 MESSAGE_SPAM). 토큰을 보내면 그날 첫 글에 코인 10개(rewardCoin, totalCoin).")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GuestbookPostResponse post(@Valid @RequestBody GuestbookRequest request) {
        return guestbookService.post(currentUserIdOrNull(), request);
    }

    /** 로그인하지 않았거나 토큰이 만료됐으면 null (방명록은 그대로 남기고 보상만 없다) */
    private static Long currentUserIdOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof Long userId ? userId : null;
    }
}
