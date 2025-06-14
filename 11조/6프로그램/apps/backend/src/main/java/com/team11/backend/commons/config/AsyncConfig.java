package com.team11.backend.commons.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {
    // 필요한 경우 ThreadPoolTaskExecutor 등을 Bean으로 등록
}
