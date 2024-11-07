package it.gov.pagopa.notifier.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
public class SchedulerConfiguration {
    @Bean
    public ScheduledExecutorService scheduler(){
        return  Executors.newScheduledThreadPool(3);
    }

}