package it.gov.pagopa.notifier.configuration;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class KafkaGroupConfig {


    public String getGroup() {
        return "group-test-" + UUID.randomUUID();
    }
}
