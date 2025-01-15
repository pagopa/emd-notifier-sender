package it.gov.pagopa.notifier.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:application.yml")
public class KafkaConfig {

    @Bean
    public KafkaGroupConfig kafkaGroupConfig() {
        return new KafkaGroupConfig();
    }
}
