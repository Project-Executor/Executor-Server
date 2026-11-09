package com.seyoung5309.executorserver.global.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.properties의 jwt.* 설정.
 *
 * @param secret                     Base64로 인코딩한 서명 키 (256bit 이상)
 * @param accessTokenValiditySeconds  Access Token 유효기간 (초)
 * @param refreshTokenValiditySeconds Refresh Token 유효기간 (초)
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        long accessTokenValiditySeconds,
        long refreshTokenValiditySeconds
) {
}
