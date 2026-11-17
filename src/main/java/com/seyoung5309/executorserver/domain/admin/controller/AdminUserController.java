package com.seyoung5309.executorserver.domain.admin.controller;

import com.seyoung5309.executorserver.domain.admin.dto.AdminUserResponse;
import com.seyoung5309.executorserver.domain.admin.service.AdminUserService;
import com.seyoung5309.executorserver.global.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "관리자 - 유저", description = "ADMIN 권한 필요")
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "유저 목록·검색",
            description = "keyword는 닉네임·이메일 일부 또는 사용자 ID. 빼면 전체. role(USER, ADMIN)을 빼면 전체. "
                    + "최근 가입순(size 1~50, 기본 20). 관리자 본인도 나온다.")
    @GetMapping
    public PageResponse<AdminUserResponse> search(
            @RequestParam(required = false)
            @Size(max = 100, message = "검색어는 100자 이하여야 합니다.")
            String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page는 0 이상이어야 합니다.") int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "size는 1 이상이어야 합니다.")
            @Max(value = 50, message = "size는 50 이하여야 합니다.")
            int size) {
        return adminUserService.search(keyword, AdminUserService.parseRole(role), page, size);
    }
}
