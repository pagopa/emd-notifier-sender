package it.gov.pagopa.common.reactive.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.common.reactive.kafka.consumer.BaseKafkaConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.Consumer;

@Slf4j
class TestKafkaConsumer extends BaseKafkaConsumer<String, String> {

    protected TestKafkaConsumer(String applicationName) {
        super(applicationName);
    }
    @Override
    protected ObjectReader getObjectReader() {
        return new ObjectMapper().readerFor(String.class);
    }
    @Override
    protected Consumer<Throwable> onDeserializationError(Message<String> message) {
        return e -> log.info("Unexpected JSON : {}", e.getMessage());
    }

    @Override
    protected Mono<String> execute(String payload, Message<String> message, Map<String, Object> ctx) {
        if ("error".equals(payload)) {
            return Mono.error(new RuntimeException("Error"));
        }
        return Mono.just("Processed: " + payload);
    }
}

