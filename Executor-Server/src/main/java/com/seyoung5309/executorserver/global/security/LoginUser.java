package com.seyoung5309.executorserver.global.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러 파라미터에 붙이면 로그인한 사용자의 ID(Long)를 넣어준다.
 * <pre>
 * &#64;GetMapping("/api/profiles/me")
 * public ProfileResponse me(&#64;LoginUser Long userId) { ... }
 * </pre>
 * 인증이 필요한 URL에서만 쓴다. 인증 정보가 없으면 401 UNAUTHORIZED.
 * Swagger 문서에는 나타나지 않는다 (SwaggerConfig에서 제외).
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginUser {
}
