package com.seyoung5309.executorserver.domain.mail.controller;

import com.seyoung5309.executorserver.domain.mail.dto.ClaimAllResponse;
import com.seyoung5309.executorserver.domain.mail.dto.ClaimResponse;
import com.seyoung5309.executorserver.domain.mail.dto.MailResponse;
import com.seyoung5309.executorserver.domain.mail.service.MailService;
import com.seyoung5309.executorserver.global.response.PageResponse;
import com.seyoung5309.executorserver.global.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "우편", description = "시스템이 보낸 보상 우편")
@RestController
@RequestMapping("/api/mails")
@RequiredArgsConstructor
public class MailController {

    private final MailService mailService;

    @Operation(summary = "우편 목록", description = "만료되지 않은 우편, 최신순(size 1~50, 기본 20). 받은 우편도 claimed = true로 나온다.")
    @GetMapping
    public PageResponse<MailResponse> getMails(
            @LoginUser Long userId,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page는 0 이상이어야 합니다.") int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "size는 1 이상이어야 합니다.")
            @Max(value = 50, message = "size는 50 이하여야 합니다.")
            int size) {
        return mailService.getMails(userId, page, size);
    }

    @Operation(summary = "우편 받기",
            description = "한 번만 받을 수 있다(409 ALREADY_CLAIMED), 만료되면 409 MAIL_EXPIRED. "
                    + "이미 가진 스킨·테마는 가격만큼 코인으로 바꿔 준다(convertedCoin).")
    @PostMapping("/{mailId}/claim")
    public ClaimResponse claim(@LoginUser Long userId, @PathVariable Long mailId) {
        return mailService.claim(userId, mailId);
    }

    @Operation(summary = "우편 모두 받기", description = "받을 수 있는 우편을 오래된 순으로 모두 받는다. 없으면 claimedCount = 0.")
    @PostMapping("/claim-all")
    public ClaimAllResponse claimAll(@LoginUser Long userId) {
        return mailService.claimAll(userId);
    }
}
