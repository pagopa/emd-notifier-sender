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
		SpringApplication.run(EmdNotifierSender.class, args);
	}

}
