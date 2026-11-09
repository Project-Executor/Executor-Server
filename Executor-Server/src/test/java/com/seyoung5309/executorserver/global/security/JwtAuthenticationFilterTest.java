package com.seyoung5309.executorserver.global.security;

import com.seyoung5309.executorserver.domain.auth.entity.Role;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationFilterTest {

    private static final String SECRET = "dGVzdC1vbmx5LWp3dC1zZWNyZXQta2V5LWZvci1leGVjdXRvci1zZXJ2ZXItdGVzdHM=";
    private static final JwtProperties PROPERTIES = new JwtProperties(SECRET, 1800, 1_209_600);

    private final JwtProvider provider = new JwtProvider(PROPERTIES);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(provider);

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 유효한_토큰이면_userId와_권한이_SecurityContext에_들어간다() throws Exception {
        MockHttpServletRequest request = requestWith("Bearer " + provider.createAccessToken(3L, Role.ADMIN));
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(3L);
        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_ADMIN");
        assertThat(chain.getRequest()).as("다음 필터로 넘어가야 한다").isNotNull();
    }

    @Test
    void 토큰이_없으면_인증_없이_통과하고_오류도_남기지_않는다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute(JwtAuthenticationFilter.ERROR_ATTRIBUTE)).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void 만료된_토큰이면_인증_없이_통과하고_TOKEN_EXPIRED를_남긴다() throws Exception {
        JwtProvider past = new JwtProvider(PROPERTIES,
                Clock.fixed(Instant.now().minus(Duration.ofHours(1)), ZoneOffset.UTC));
        MockHttpServletRequest request = requestWith("Bearer " + past.createAccessToken(3L, Role.USER));
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute(JwtAuthenticationFilter.ERROR_ATTRIBUTE)).isEqualTo(ErrorCode.TOKEN_EXPIRED);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void 잘못된_토큰이면_UNAUTHORIZED를_남긴다() throws Exception {
        MockHttpServletRequest request = requestWith("Bearer garbage.token.value");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute(JwtAuthenticationFilter.ERROR_ATTRIBUTE)).isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    void Bearer_접두사가_없으면_토큰으로_보지_않는다() throws Exception {
        MockHttpServletRequest request = requestWith(provider.createAccessToken(3L, Role.USER));

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute(JwtAuthenticationFilter.ERROR_ATTRIBUTE)).isNull();
    }

    private MockHttpServletRequest requestWith(String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", authorization);
        return request;
    }
}
