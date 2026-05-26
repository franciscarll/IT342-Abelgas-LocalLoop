package edu.cit.abelgas.localloop.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Enables Spring's @Async support so that methods annotated with @Async
 * (e.g. EmailServiceImpl.sendWelcomeEmail) run in a background thread
 * instead of blocking the HTTP request thread.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    // Default SimpleAsyncTaskExecutor is fine for email sending.
    // Add a custom ThreadPoolTaskExecutor here if you need finer control later.
}