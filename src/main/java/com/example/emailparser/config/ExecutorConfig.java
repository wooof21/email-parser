package com.example.emailparser.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ExecutorConfig {

    @Value("${threadPollExecutor.corePool:3}")
    Integer corePool;

    @Value("${threadPollExecutor.maxPool:10}")
    Integer maxPool;

    @Value("${threadPollExecutor.queueCapacity:100}")
    Integer queueCapacity;

    @Value("${threadPollExecutor.keepAliveSeconds:600}")
    Integer keepAliveSeconds;

    @Value("${threadPollExecutor.poolPrefix}")
    String poolPrefix;

    @Bean(name = "emailErrorPool")
    @Primary
    public TaskExecutor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePool);
        executor.setMaxPoolSize(maxPool);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setThreadNamePrefix(poolPrefix);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }
}
