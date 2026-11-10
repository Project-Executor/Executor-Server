package com.seyoung5309.executorserver.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** 현재 시각은 Clock 빈으로 얻는다 (UTC). 테스트에서 시간을 고정할 수 있다. */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
