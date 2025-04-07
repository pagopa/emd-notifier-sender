package it.gov.pagopa.notifier;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = "it.gov.pagopa")
@EnableFeignClients(basePackages = "it.gov.pagopa.notifier.connector")
@EnableScheduling
public class EmdNotifierSender {

	public static void main(String[] args) {
		SpringApplication.run(EmdNotifierSender.class, args);
	}

}
