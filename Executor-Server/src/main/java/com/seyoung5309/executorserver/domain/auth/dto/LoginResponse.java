package com.seyoung5309.executorserver.domain.auth.dto;

import com.seyoung5309.executorserver.domain.auth.entity.Role;

/**
 * 로그인 응답.
 * user.hasCharacterName이 false면 클라이언트는 튜토리얼의 이름 입력 단계로 보낸다.
 */
public record LoginResponse(
        String accessToken,
        String refreshToken,
        long accessTokenExpiresIn,
        LoginUserInfo user
) {

    public record LoginUserInfo(Long userId, String nickname, Role role, boolean hasCharacterName) {
    }
}
