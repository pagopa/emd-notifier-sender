package it.gov.pagopa.notifier.event.consumer;


import it.gov.pagopa.notifier.service.MessageCoreConsumerService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;


@Configuration
public class MessageCoreConsumer {


    @Bean
    public Consumer<Flux<Message<String>>> consumerMessage(MessageCoreConsumerService consumerService) {
        return consumerService::execute;
    }

}
