package it.gov.pagopa.notifier.event.consumer;


import it.gov.pagopa.notifier.service.MessageConsumerService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;


@Configuration
public class MessageConsumer {


    @Bean
    public Consumer<Flux<Message<String>>> consumerCommands(MessageConsumerService consumerService) {
        return consumerService::execute;
    }

}
