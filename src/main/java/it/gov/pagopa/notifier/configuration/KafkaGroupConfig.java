package it.gov.pagopa.notifier.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class KafkaGroupConfig {

    @Value("${consumerMessageGroup}")
    private String consumerMessageGroup;

    public String getGroup() {
        return consumerMessageGroup != null ? consumerMessageGroup : "group-" + UUID.randomUUID().toString();
    }
}
