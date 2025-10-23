package it.gov.pagopa.notifier.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Configuration class for scheduled task execution.
 * Provides a thread pool for managing scheduled operations.
 */
@Configuration
public class SchedulerConfiguration {

    /**
     * Creates a ScheduledExecutorService with a fixed thread pool.
     * The pool is configured with 3 threads to handle concurrent scheduled tasks.
     *
     * @return a ScheduledExecutorService instance with 3 threads
     */
    @Bean
    public ScheduledExecutorService scheduler(){
        return  Executors.newScheduledThreadPool(3);
    }

}