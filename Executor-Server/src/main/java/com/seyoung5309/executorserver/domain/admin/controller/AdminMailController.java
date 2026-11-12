package com.seyoung5309.executorserver.domain.admin.controller;

import com.seyoung5309.executorserver.domain.admin.dto.AdminMailRequest;
import com.seyoung5309.executorserver.domain.admin.dto.AdminMailResponse;
import com.seyoung5309.executorserver.domain.admin.service.AdminMailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "관리자 - 우편", description = "ADMIN 권한 필요")
@RestController
@RequestMapping("/api/admin/mails")
@RequiredArgsConstructor
public class AdminMailController {

    private final AdminMailService adminMailService;

    @Operation(summary = "우편 발송",
            description = "toAll이 true면 모든 사용자에게, 아니면 receiverIds(최대 1000명, 중복 제거)에게 한 통씩. "
                    + "첨부 아이템이 없으면 수량 0, 스킨·테마는 1개, 힌트는 1~99개. expiresAt은 null(무기한) 또는 미래 시각.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminMailResponse send(@Valid @RequestBody AdminMailRequest request) {
        return adminMailService.send(request);
    }
}
