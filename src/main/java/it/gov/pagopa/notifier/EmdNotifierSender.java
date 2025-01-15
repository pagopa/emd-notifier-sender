package it.gov.pagopa.notifier;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.UUID;

@Slf4j
@SpringBootApplication(scanBasePackages = "it.gov.pagopa")
@EnableFeignClients(basePackages = "it.gov.pagopa.notifier.connector")
public class EmdNotifierSender {

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(EmdNotifierSender.class);
		application.addInitializers(applicationContext -> {
            ConfigurableEnvironment environment = applicationContext.getEnvironment();
            String dynamicGroup = "consumer-group-" + UUID.randomUUID();
            environment.getSystemProperties().put("KAFKA_MESSAGE_CORE_GROUP_IN_TEST", dynamicGroup);
            log.info("Dynamically set Kafka group: " + dynamicGroup);
        });

		SpringApplication.run(EmdNotifierSender.class, args);
	}

}
