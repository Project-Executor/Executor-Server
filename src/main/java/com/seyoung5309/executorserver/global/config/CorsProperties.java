package com.seyoung5309.executorserver.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 브라우저에서 이 API를 부를 수 있는 다른 주소(Origin) 목록. 관리자 웹처럼 서버와 주소가 다른 웹이 쓴다.
 * 게임(Unity)은 브라우저가 아니라서 CORS와 상관없다.
 * <p>
 * application.properties의 {@code cors.allowed-origins}. 배포 환경에서는 Railway Variables의
 * {@code CORS_ALLOWED_ORIGINS}(쉼표로 여러 개)로 바꾼다. 끝에 / 없이 scheme + 호스트(+ 포트)만 적는다.
 */
@ConfigurationProperties(prefix = "cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        allowedOrigins = allowedOrigins == null ? List.of()
                : allowedOrigins.stream().map(String::strip).filter(s -> !s.isEmpty()).map(CorsProperties::trimSlash).toList();
    }

    private static String trimSlash(String origin) {
        return origin.endsWith("/") ? origin.substring(0, origin.length() - 1) : origin;
    }
}
