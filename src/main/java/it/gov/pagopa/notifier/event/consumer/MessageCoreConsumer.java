package it.gov.pagopa.notifier.event.consumer;


import it.gov.pagopa.notifier.service.MessageCoreConsumerService;
import it.gov.pagopa.notifier.service.MessageCoreConsumerServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;


/**
 * Configuration class for the Message Core Consumer.
 */
@Configuration
public class MessageCoreConsumer {

    /**
     *
     * Bean definition for consuming messages. <br>
     * Delegates message processing to execute of  {@link MessageCoreConsumerServiceImpl}.
     *
     * @param consumerService the service that processes the consumed messages
     * @return a Consumer that processes one message before returning to Kafka
     */
    @Bean
    public Consumer<Message<String>> consumerMessage(MessageCoreConsumerService consumerService) {
        return consumerService::execute;
    }

}
