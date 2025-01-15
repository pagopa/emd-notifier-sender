package it.gov.pagopa.notifier.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
public class KafkaConfig {

    @Bean
    public String consumerMessageGroup() {
        return "group-test-" + UUID.randomUUID();
    }
}