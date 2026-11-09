package com.seyoung5309.executorserver.global.security;

import com.seyoung5309.executorserver.domain.auth.entity.Role;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 발급과 검증.
 * <ul>
 *   <li>Access Token : sub = userId, role, type = "access"</li>
 *   <li>Refresh Token: sub = userId, type = "refresh", jti = 무작위 ID (같은 초에 발급해도 값이 달라지도록)</li>
 * </ul>
 */
@Component
public class JwtProvider {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final long accessTokenValiditySeconds;
    private final long refreshTokenValiditySeconds;
    private final Clock clock;

    /** 생성자가 두 개라서 Spring이 쓸 생성자를 지정한다 */
    @Autowired
    public JwtProvider(JwtProperties properties) {
        this(properties, Clock.systemUTC());
    }

    /** 테스트에서 시간을 고정하기 위한 생성자 */
    JwtProvider(JwtProperties properties, Clock clock) {
        if (properties.secret() == null || properties.secret().isBlank()) {
            throw new IllegalStateException("jwt.secret(JWT_SECRET)이 설정되지 않았습니다.");
        }
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret()));
        this.accessTokenValiditySeconds = properties.accessTokenValiditySeconds();
        this.refreshTokenValiditySeconds = properties.refreshTokenValiditySeconds();
        this.clock = clock;
    }

    public String createAccessToken(Long userId, Role role) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_ROLE, role.name())
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTokenValiditySeconds)))
                .signWith(key)
                .compact();
    }

    public String createRefreshToken(Long userId) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(refreshTokenValiditySeconds)))
                .signWith(key)
                .compact();
    }

    /**
     * Access Token을 검증하고 내용을 꺼낸다.
     *
     * @throws BusinessException TOKEN_EXPIRED(만료), UNAUTHORIZED(위조·형식 오류·Refresh Token을 넣은 경우)
     */
    public AccessTokenClaims parseAccessToken(String token) {
        Claims claims = parse(token, ErrorCode.TOKEN_EXPIRED, ErrorCode.UNAUTHORIZED);
        if (!TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        try {
            Long userId = Long.valueOf(claims.getSubject());
            Role role = Role.valueOf(claims.get(CLAIM_ROLE, String.class));
            return new AccessTokenClaims(userId, role);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    /**
     * Refresh Token을 검증하고 userId를 꺼낸다. (Redis에 저장된 값과의 비교는 서비스에서 한다)
     *
     * @throws BusinessException INVALID_REFRESH_TOKEN (만료·위조·Access Token을 넣은 경우)
     */
    public Long parseRefreshToken(String token) {
        Claims claims = parse(token, ErrorCode.INVALID_REFRESH_TOKEN, ErrorCode.INVALID_REFRESH_TOKEN);
        if (!TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        try {
            return Long.valueOf(claims.getSubject());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    public long getAccessTokenValiditySeconds() {
        return accessTokenValiditySeconds;
    }

    public long getRefreshTokenValiditySeconds() {
        return refreshTokenValiditySeconds;
    }

    private Claims parse(String token, ErrorCode whenExpired, ErrorCode whenInvalid) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new BusinessException(whenExpired);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(whenInvalid);
        }
    }

    public record AccessTokenClaims(Long userId, Role role) {
    }
}
