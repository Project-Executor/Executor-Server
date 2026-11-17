package com.seyoung5309.executorserver.domain.admin.controller;

import com.seyoung5309.executorserver.domain.admin.dto.AdminCoinRequest;
import com.seyoung5309.executorserver.domain.admin.dto.AdminCoinResponse;
import com.seyoung5309.executorserver.domain.admin.dto.AdminRoleRequest;
import com.seyoung5309.executorserver.domain.admin.dto.AdminSuspendRequest;
import com.seyoung5309.executorserver.domain.admin.dto.AdminUserDetailResponse;
import com.seyoung5309.executorserver.domain.admin.dto.AdminUserResponse;
import com.seyoung5309.executorserver.domain.admin.service.AdminUserService;
import com.seyoung5309.executorserver.global.response.PageResponse;
import com.seyoung5309.executorserver.global.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @Operation(summary = "ID로 여러 명 조회",
            description = "ids=1,2,3 (한 번에 100개까지, 중복은 한 번만). ID 순. 없는 ID는 결과에서 빠진다.")
    @GetMapping("/by-ids")
    public List<AdminUserResponse> lookup(@RequestParam(required = false) List<Long> ids) {
        return adminUserService.lookup(ids);
    }

    @Operation(summary = "유저 상세",
            description = "계정·코인·장착 아이템·공식 스테이지 기록(클리어 수, 별 합계)·보유 아이템.")
    @GetMapping("/{userId}")
    public AdminUserDetailResponse getDetail(@PathVariable Long userId) {
        return adminUserService.getDetail(userId);
    }

    @Operation(summary = "코인 조정",
            description = "amount만큼 더한다(음수면 뺀다, 0 불가, -1,000,000~1,000,000). 빼서 0 미만이 되면 409 NOT_ENOUGH_COIN. "
                    + "응답은 조정한 뒤의 코인.")
    @PatchMapping("/{userId}/coin")
    public AdminCoinResponse adjustCoin(@PathVariable Long userId, @Valid @RequestBody AdminCoinRequest request) {
        return adminUserService.adjustCoin(userId, request.amount());
    }

    @Operation(summary = "권한 변경",
            description = "role은 USER 또는 ADMIN. 자기 자신은 바꿀 수 없다(400). "
                    + "새 권한은 그 사용자의 다음 토큰 재발급(최대 30분 뒤)부터 적용된다.")
    @PatchMapping("/{userId}/role")
    public AdminUserResponse changeRole(@LoginUser Long adminId, @PathVariable Long userId,
                                        @Valid @RequestBody AdminRoleRequest request) {
        return adminUserService.changeRole(adminId, userId, request.role());
    }

    @Operation(summary = "계정 정지",
            description = "until(ISO-8601 UTC)까지 정지, null이면 영구. reason 필수(200자 이하). 이미 정지 중이면 새 값으로 바꾼다. "
                    + "자기 자신은 400. 로그인·토큰 재발급이 막힌다(이미 받은 Access Token은 최대 30분 유효).")
    @PutMapping("/{userId}/suspension")
    public AdminUserResponse suspend(@LoginUser Long adminId, @PathVariable Long userId,
                                     @Valid @RequestBody AdminSuspendRequest request) {
        return adminUserService.suspend(adminId, userId, request.until(), request.reason());
    }

    @Operation(summary = "정지 해제", description = "정지 중이 아니어도 200.")
    @DeleteMapping("/{userId}/suspension")
    public AdminUserResponse unsuspend(@PathVariable Long userId) {
        return adminUserService.unsuspend(userId);
    }
}
