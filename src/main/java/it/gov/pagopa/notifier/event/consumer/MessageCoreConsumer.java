package it.gov.pagopa.notifier.event.consumer;


import it.gov.pagopa.notifier.service.MessageCoreConsumerService;
import it.gov.pagopa.notifier.service.NotifyErrorConsumerService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;


/**
 * Configuration class for the Message Core Consumer.
 */
@Configuration
public class MessageCoreConsumer {

    /**
     *
     * Bean definition for consuming messages. <br>
     * Delegates message processing to {@link MessageCoreConsumerService#execute(Flux)}.
     *
     * @param consumerService the service that processes the consumed messages
     * @return a Consumer that processes a Flux of Messages containing String payloads
     */
    @Bean
    public Consumer<Flux<Message<String>>> consumerMessage(MessageCoreConsumerService consumerService) {
        return consumerService::execute;
    }

}
