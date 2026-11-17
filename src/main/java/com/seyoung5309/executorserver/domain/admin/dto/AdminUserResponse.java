package com.seyoung5309.executorserver.domain.admin.dto;

import com.seyoung5309.executorserver.domain.auth.entity.Role;
import com.seyoung5309.executorserver.domain.profile.entity.User;

import java.time.Instant;

/** 관리자 유저 목록 항목. 일반 사용자에게는 보이지 않는 이메일·권한을 담는다 */
public record AdminUserResponse(
        Long userId,
        String email,
        /** 가입 후 닉네임을 정하기 전이면 null */
        String nickname,
        Role role,
        int coin,
        Instant createdAt,
        /** 정지 기한. null이면 정지 아님, 9999-12-31T23:59:59Z면 영구. 지난 시각이면 이미 풀린 것 */
        Instant suspendedUntil,
        String suspendReason
) {
    /** user.account가 읽혀 있어야 한다 (fetch join) */
    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(user.getUserId(), user.getAccount().getEmail(), user.getNickname(),
                user.getAccount().getRole(), user.getCoin(), user.getAccount().getCreatedAt(),
                user.getAccount().getSuspendedUntil(), user.getAccount().getSuspendReason());
    }
}
