package com.seyoung5309.executorserver.domain.auth.dto;

/** 토큰 재발급 응답 */
public record TokenResponse(String accessToken, String refreshToken, long accessTokenExpiresIn) {
}
