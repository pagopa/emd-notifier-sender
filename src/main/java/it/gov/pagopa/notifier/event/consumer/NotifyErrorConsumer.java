package it.gov.pagopa.notifier.event.consumer;


import it.gov.pagopa.notifier.service.NotifyErrorConsumerService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

/**
 * Configuration class for the Notify Error Consumer.
 * Defines a Spring Cloud Stream consumer bean that processes notification error messages.
 */
@Configuration
public class NotifyErrorConsumer {

    /**
     * Bean definition for consuming notification error messages. <br>
     * Delegates message processing to {@link NotifyErrorConsumerService#execute(Flux)}.
     *
     * @param consumerService the service that handles the consumption logic
     * @return a Consumer that processes a Flux of notification error messages
     */
    @Bean
    public Consumer<Flux<Message<String>>> consumerNotify(NotifyErrorConsumerService consumerService) {
        return consumerService::execute;
    }

}
