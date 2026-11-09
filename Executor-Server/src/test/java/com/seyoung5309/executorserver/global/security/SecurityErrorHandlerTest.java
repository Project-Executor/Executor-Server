package com.seyoung5309.executorserver.global.security;

import com.seyoung5309.executorserver.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityErrorHandlerTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private final SecurityErrorHandler handler = new SecurityErrorHandler(objectMapper);

    @Test
    void 토큰이_없으면_401_UNAUTHORIZED() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.commence(new MockHttpServletRequest(), response, new InsufficientAuthenticationException("x"));

        assertError(response, 401, "UNAUTHORIZED");
    }

    @Test
    void 필터가_남긴_TOKEN_EXPIRED를_그대로_쓴다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(JwtAuthenticationFilter.ERROR_ATTRIBUTE, ErrorCode.TOKEN_EXPIRED);
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.commence(request, response, new InsufficientAuthenticationException("x"));

        assertError(response, 401, "TOKEN_EXPIRED");
    }

    @Test
    void 권한이_없으면_403_FORBIDDEN() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(new MockHttpServletRequest(), response, new AccessDeniedException("x"));

        assertError(response, 403, "FORBIDDEN");
    }

    @Test
    void 한글_메시지가_깨지지_않는다() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(new MockHttpServletRequest(), response, new AccessDeniedException("x"));

        assertThat(response.getCharacterEncoding()).isEqualToIgnoringCase("UTF-8");
        assertThat(body(response).get("message").asString()).isEqualTo(ErrorCode.FORBIDDEN.getMessage());
    }

    private void assertError(MockHttpServletResponse response, int status, String code) throws Exception {
        assertThat(response.getStatus()).isEqualTo(status);
        assertThat(response.getContentType()).startsWith("application/json");
        JsonNode body = body(response);
        assertThat(body.get("status").asInt()).isEqualTo(status);
        assertThat(body.get("code").asString()).isEqualTo(code);
        assertThat(body.get("message").asString()).isNotBlank();
    }

    private JsonNode body(MockHttpServletResponse response) throws Exception {
        return objectMapper.readTree(response.getContentAsString());
    }
}
