package com.seyoung5309.executorserver.global.security;

import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * &#64;LoginUser Long 파라미터에 SecurityContext의 userId를 넣는다.
 * JwtAuthenticationFilter가 principal에 userId(Long)를 저장해 둔다.
 */
public class LoginUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        if (!parameter.hasParameterAnnotation(LoginUser.class)) {
            return false;
        }
        if (!Long.class.equals(parameter.getParameterType())) {
            throw new IllegalStateException("@LoginUser는 Long 타입 파라미터에만 쓸 수 있습니다: " + parameter);
        }
        return true;
    }

    @Override
    public Long resolveArgument(MethodParameter parameter,
                                ModelAndViewContainer mavContainer,
                                NativeWebRequest webRequest,
                                WebDataBinderFactory binderFactory) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // 로그인하지 않은 요청의 principal은 "anonymousUser" 문자열이므로 Long인지로 판단한다
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }
}
