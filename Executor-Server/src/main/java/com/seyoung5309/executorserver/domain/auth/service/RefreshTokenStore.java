package com.seyoung5309.executorserver.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * Refresh Token 저장소 (Redis).
 * 키: refresh:{userId}, 값: Refresh Token 원문, TTL: Refresh Token 유효기간.
 * 사용자당 하나만 저장하므로 다른 기기에서 로그인하면 이전 기기의 Refresh Token은 무효가 된다.
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenStore {

    private static final String KEY_PREFIX = "refresh:";

    /**
     * 저장된 값이 기존 토큰과 같을 때만 새 토큰으로 바꾼다 (원자적으로).
     * 같은 Refresh Token으로 동시에 재발급을 요청해도 한 번만 성공한다.
     */
    private static final RedisScript<Long> ROTATE_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                redis.call('SET', KEYS[1], ARGV[2], 'EX', ARGV[3])
                return 1
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    public void save(Long userId, String refreshToken, Duration ttl) {
        redisTemplate.opsForValue().set(key(userId), refreshToken, ttl);
    }

    /** @return 기존 토큰이 저장된 값과 같아서 교체했으면 true */
    public boolean rotate(Long userId, String oldToken, String newToken, Duration ttl) {
        Long result = redisTemplate.execute(ROTATE_SCRIPT, List.of(key(userId)),
                oldToken, newToken, String.valueOf(ttl.toSeconds()));
        return Long.valueOf(1L).equals(result);
    }

    public void delete(Long userId) {
        redisTemplate.delete(key(userId));
    }

    private String key(Long userId) {
        return KEY_PREFIX + userId;
    }
}
