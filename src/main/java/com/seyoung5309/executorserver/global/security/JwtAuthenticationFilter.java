package com.seyoung5309.executorserver.global.security;

import com.seyoung5309.executorserver.global.exception.BusinessException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 요청마다 Authorization: Bearer {accessToken} 헤더를 확인한다.
 * <ul>
 *   <li>토큰이 유효하면 SecurityContext에 인증 정보(principal = userId, 권한 = ROLE_{role})를 넣는다.</li>
 *   <li>토큰이 없거나 잘못됐으면 인증 없이 다음 필터로 넘긴다.
 *       잘못된 이유(ErrorCode)는 요청 속성에 남겨서, 인증이 필요한 URL이면 401 응답에 쓴다.</li>
 * </ul>
 * 공개 URL은 토큰이 잘못돼도 그대로 통과한다.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** 토큰 검증 실패 이유(ErrorCode)를 담는 요청 속성 이름 */
    public static final String ERROR_ATTRIBUTE = "jwt.error";

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);

        if (token != null) {
            try {
                JwtProvider.AccessTokenClaims claims = jwtProvider.parseAccessToken(token);
                var authentication = new UsernamePasswordAuthenticationToken(
                        claims.userId(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + claims.role().name()))
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (BusinessException e) {
                SecurityContextHolder.clearContext();
                request.setAttribute(ERROR_ATTRIBUTE, e.getErrorCode());
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }
}
