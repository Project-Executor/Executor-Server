package com.seyoung5309.executorserver.global.security;

import com.seyoung5309.executorserver.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LoginUserArgumentResolverTest {

    private final LoginUserArgumentResolver resolver = new LoginUserArgumentResolver();

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
            .setCustomArgumentResolvers(resolver)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 로그인한_사용자의_ID가_파라미터로_들어온다() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(42L, null, AuthorityUtils.createAuthorityList("ROLE_USER")));

        mockMvc.perform(get("/test/me"))
                .andExpect(status().isOk())
                .andExpect(content().string("42"));
    }

    @Test
    void 익명_사용자면_401_UNAUTHORIZED() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken("key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        mockMvc.perform(get("/test/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void 인증_정보가_아예_없으면_401_UNAUTHORIZED() throws Exception {
        mockMvc.perform(get("/test/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void LoginUser가_없는_파라미터는_처리하지_않는다() throws Exception {
        MethodParameter plain = parameterOf("plain", Long.class);

        assertThat(resolver.supportsParameter(plain)).isFalse();
    }

    @Test
    void Long이_아닌_타입에_LoginUser를_붙이면_개발_중에_바로_알_수_있게_실패한다() throws Exception {
        MethodParameter wrongType = parameterOf("wrongType", String.class);

        assertThatThrownBy(() -> resolver.supportsParameter(wrongType))
                .isInstanceOf(IllegalStateException.class);
    }

    private MethodParameter parameterOf(String methodName, Class<?> type) throws NoSuchMethodException {
        return new MethodParameter(Signatures.class.getDeclaredMethod(methodName, type), 0);
    }

    @RestController
    static class TestController {
        @GetMapping("/test/me")
        Long me(@LoginUser Long userId) {
            return userId;
        }
    }

    /** supportsParameter 검사용 메서드 시그니처 모음 */
    @SuppressWarnings("unused")
    static class Signatures {
        void plain(Long userId) {
        }

        void wrongType(@LoginUser String userId) {
        }
    }
}
