package it.gov.pagopa.notifier.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "delete")
public class DeleteProperties {
    private int batchSize;
    private int intervalMs;
    private int retentionPeriodDays;
    private String batchExecutionCron;
}