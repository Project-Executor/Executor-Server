package com.seyoung5309.executorserver.domain.auth.dto;

import com.seyoung5309.executorserver.domain.auth.entity.Role;

/**
 * 로그인 응답.
 * user.hasNickname이 false면(가입 후 아직 닉네임을 정하지 않음) 클라이언트는 튜토리얼 1스테이지의 닉네임 입력으로 보낸다.
 */
public record LoginResponse(
        String accessToken,
        String refreshToken,
        long accessTokenExpiresIn,
        LoginUserInfo user
) {

    public record LoginUserInfo(Long userId, String nickname, Role role, boolean hasNickname) {
    }
}
