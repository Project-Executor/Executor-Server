package com.seyoung5309.executorserver.global.security;

import com.seyoung5309.executorserver.domain.auth.entity.Role;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtProviderTest {

    private static final String SECRET = "dGVzdC1vbmx5LWp3dC1zZWNyZXQta2V5LWZvci1leGVjdXRvci1zZXJ2ZXItdGVzdHM=";
    private static final String OTHER_SECRET = "b3RoZXItc2VjcmV0LWtleS1mb3ItdGFtcGVyaW5nLXRlc3QtZXhlY3V0b3ItMTIzNDU=";
    private static final Instant NOW = Instant.parse("2026-11-09T00:00:00Z");

    private final JwtProperties properties = new JwtProperties(SECRET, 1800, 1_209_600);
    private final JwtProvider provider = new JwtProvider(properties, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void Access_Token을_발급하고_userId와_role을_꺼낸다() {
        String token = provider.createAccessToken(7L, Role.ADMIN);

        JwtProvider.AccessTokenClaims claims = provider.parseAccessToken(token);

        assertThat(claims.userId()).isEqualTo(7L);
        assertThat(claims.role()).isEqualTo(Role.ADMIN);
    }

    @Test
    void 만료된_Access_Token은_TOKEN_EXPIRED() {
        String token = provider.createAccessToken(7L, Role.USER);
        JwtProvider later = providerAt(NOW.plus(Duration.ofMinutes(31)));

        assertErrorCode(() -> later.parseAccessToken(token), ErrorCode.TOKEN_EXPIRED);
    }

    @Test
    void 다른_키로_서명된_토큰은_UNAUTHORIZED() {
        JwtProvider other = new JwtProvider(new JwtProperties(OTHER_SECRET, 1800, 1_209_600),
                Clock.fixed(NOW, ZoneOffset.UTC));
        String forged = other.createAccessToken(7L, Role.ADMIN);

        assertErrorCode(() -> provider.parseAccessToken(forged), ErrorCode.UNAUTHORIZED);
    }

    @Test
    void 형식이_잘못된_토큰은_UNAUTHORIZED() {
        assertErrorCode(() -> provider.parseAccessToken("not-a-jwt"), ErrorCode.UNAUTHORIZED);
    }

    @Test
    void Refresh_Token을_Access_Token으로_쓰면_UNAUTHORIZED() {
        String refresh = provider.createRefreshToken(7L);

        assertErrorCode(() -> provider.parseAccessToken(refresh), ErrorCode.UNAUTHORIZED);
    }

    @Test
    void Refresh_Token을_발급하고_userId를_꺼낸다() {
        String refresh = provider.createRefreshToken(7L);

        assertThat(provider.parseRefreshToken(refresh)).isEqualTo(7L);
    }

    @Test
    void 같은_시각에_발급한_Refresh_Token도_서로_다르다() {
        assertThat(provider.createRefreshToken(7L)).isNotEqualTo(provider.createRefreshToken(7L));
    }

    @Test
    void Access_Token을_Refresh_Token으로_쓰면_INVALID_REFRESH_TOKEN() {
        String access = provider.createAccessToken(7L, Role.USER);

        assertErrorCode(() -> provider.parseRefreshToken(access), ErrorCode.INVALID_REFRESH_TOKEN);
    }

    @Test
    void 만료된_Refresh_Token은_INVALID_REFRESH_TOKEN() {
        String refresh = provider.createRefreshToken(7L);
        JwtProvider later = providerAt(NOW.plus(Duration.ofDays(15)));

        assertErrorCode(() -> later.parseRefreshToken(refresh), ErrorCode.INVALID_REFRESH_TOKEN);
    }

    @Test
    void 서명_키가_없으면_서버가_시작되지_않는다() {
        assertThatThrownBy(() -> new JwtProvider(new JwtProperties("", 1800, 1_209_600)))
                .isInstanceOf(IllegalStateException.class);
    }

    private JwtProvider providerAt(Instant instant) {
        return new JwtProvider(properties, Clock.fixed(instant, ZoneOffset.UTC));
    }

    private void assertErrorCode(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
    }
}
