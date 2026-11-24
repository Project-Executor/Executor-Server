package com.seyoung5309.executorserver.global.config;

import com.seyoung5309.executorserver.global.security.JwtAuthenticationFilter;
import com.seyoung5309.executorserver.global.security.JwtProperties;
import com.seyoung5309.executorserver.global.security.JwtProvider;
import com.seyoung5309.executorserver.global.security.SecurityErrorHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * 보안 설정.
 * 세션 없이 JWT(Access Token)로 인증하고, 막힌 요청은 SecurityErrorHandler가 JSON으로 응답한다.
 * 다른 주소의 웹(관리자 웹)은 CORS 허용 목록(CorsProperties)에 있는 주소만 브라우저에서 부를 수 있다.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final SecurityErrorHandler securityErrorHandler;
    private final CorsProperties corsProperties;

    private static final String[] PUBLIC_URLS = {
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            // 개발용 API 테스트 페이지 (static/api-tester.html). 페이지만 공개이고, API 권한 검사는 그대로다
            "/api-tester.html",
            "/api/auth/**",
            "/api/profiles/check-nickname",
            "/api/health",
            "/error"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CORS 사전 요청(OPTIONS)도 인증보다 먼저 이 설정으로 응답한다
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // /api/auth/** 중 로그아웃만은 로그인이 필요하다 (위에서부터 먼저 맞는 규칙이 적용됨)
                        .requestMatchers("/api/auth/logout").authenticated()
                        .requestMatchers(PUBLIC_URLS).permitAll()
                        // 공지는 로그인 전 시작 화면에서도 보여 줄 수 있게 조회만 공개
                        .requestMatchers(HttpMethod.GET, "/api/notices", "/api/notices/*").permitAll()
                        // 타자 해킹 단어는 오프라인 테스트에서도 받을 수 있게 조회만 공개
                        .requestMatchers(HttpMethod.GET, "/api/word-lists/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/characters").permitAll()
                        // 게스트 플레이(전시용): 공식 작전·스테이지 조회만 공개. 결과 저장·순위는 로그인해야 한다
                        .requestMatchers(HttpMethod.GET, "/api/guest/**").permitAll()
                        // 이벤트 스테이지(전시용): 로그인 없이 순위를 보고 기록을 올린다
                        .requestMatchers(HttpMethod.GET, "/api/events/*/records").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/events/*/records").permitAll()
                        // 방명록(전시용): 로그인 없이 보고 남긴다 (토큰이 있으면 하루 첫 글 코인 보상)
                        .requestMatchers(HttpMethod.GET, "/api/guestbook/latest").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/guestbook").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                // 401(인증 없음) / 403(권한 없음)을 공통 JSON 형식으로 응답
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(securityErrorHandler)
                        .accessDeniedHandler(securityErrorHandler))
                // @Component로 등록하면 서블릿 필터로도 한 번 더 등록되므로 여기서 직접 생성한다
                .addFilterBefore(new JwtAuthenticationFilter(jwtProvider), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * CORS: 허용 목록의 주소에서 온 /api 요청만 브라우저가 응답을 읽을 수 있다.
     * 인증은 Authorization 헤더의 Bearer 토큰이라 쿠키(credentials)는 허용하지 않는다
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(corsProperties.allowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
