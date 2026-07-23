package io.github.afranusmani.urlshortener.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Enables asynchronous execution and provides a small, bounded pool for
 * recording detailed click analytics off the redirect hot path.
 *
 * <p>The redirect itself only does an atomic counter bump; the richer
 * per-click event (referrer, device, browser) is persisted on this executor so
 * a slow write never adds latency to the user-facing 302.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "clickExecutor")
    public Executor clickExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("click-");
        // Under a burst beyond the queue, run on the caller rather than drop the event.
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
