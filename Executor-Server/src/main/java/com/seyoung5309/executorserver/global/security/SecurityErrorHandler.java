package com.seyoung5309.executorserver.global.security;

import com.seyoung5309.executorserver.global.exception.ErrorCode;
import com.seyoung5309.executorserver.global.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Security 필터 단계에서 막힌 요청을 공통 오류 형식(ErrorResponse)으로 응답한다.
 * 컨트롤러까지 가지 못한 요청이라 GlobalExceptionHandler가 처리할 수 없기 때문에 따로 둔다.
 * <ul>
 *   <li>로그인이 필요한데 인증이 없음 -> 401. JwtAuthenticationFilter가 남긴 사유(TOKEN_EXPIRED 등)가 있으면 그 코드를 쓴다.</li>
 *   <li>로그인은 했지만 권한이 없음(예: USER가 /api/admin/** 호출) -> 403 FORBIDDEN</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        Object reason = request.getAttribute(JwtAuthenticationFilter.ERROR_ATTRIBUTE);
        ErrorCode errorCode = reason instanceof ErrorCode code ? code : ErrorCode.UNAUTHORIZED;
        write(response, errorCode);
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        write(response, ErrorCode.FORBIDDEN);
    }

    private void write(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(ErrorResponse.of(errorCode)));
    }
}
