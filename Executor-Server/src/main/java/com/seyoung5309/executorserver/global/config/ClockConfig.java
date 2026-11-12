package com.seyoung5309.executorserver.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;

/** 현재 시각은 Clock 빈으로 얻는다 (UTC). 테스트에서 시간을 고정할 수 있다. */
@Configuration
public class ClockConfig {

    /**
     * UTC 시계. 초 단위로 끊는다(tick).
     * <p>
     * DB 시각 컬럼은 DATETIME(초 단위)이고 MySQL은 소수점 이하를 반올림해서 저장한다.
     * 나노초가 붙은 시각을 쓰면 저장 직후 응답한 값과 나중에 DB에서 읽은 값이 최대 1초 달라지므로,
     * 처음부터 초 단위 시각만 쓰게 해서 응답과 저장 값을 항상 같게 한다.
     */
    @Bean
    public Clock clock() {
        return Clock.tick(Clock.systemUTC(), Duration.ofSeconds(1));
    }
}
