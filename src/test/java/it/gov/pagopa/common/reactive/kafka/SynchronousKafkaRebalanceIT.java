package it.gov.pagopa.common.reactive.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.common.reactive.kafka.consumer.BaseKafkaConsumer;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Testcontainers
class SynchronousKafkaRebalanceIT {
    @Container
    static final ConfluentKafkaContainer kafka = new ConfluentKafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.0")
                    .asCompatibleSubstituteFor("apache/kafka"));

    @Test
    void rebalanceWhileFirstRecordIsUnpersistedReplaysEntirePoll() throws Exception {
        String topic = "rebalance-ack-test";
        String group = "rebalance-ack-group";
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        Set<String> persisted = ConcurrentHashMap.newKeySet();

        KafkaMessageListenerContainer<String, String> first = listener(topic, group,
                processor(persisted, firstStarted, releaseFirst));
        KafkaMessageListenerContainer<String, String> replacement = listener(topic, group,
                processor(persisted, null, null));
        try (AdminClient admin = AdminClient.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafka.getBootstrapServers()))) {
            first.start();
            try (KafkaProducer<String, String> producer = new KafkaProducer<>(Map.of(
                    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                    ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                    ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class))) {
                for (int i = 0; i < 10; i++) {
                    producer.send(new ProducerRecord<>(topic, 0, null, "\"record-" + i + "\"")).get();
                }
            }

            assertThat(firstStarted.await(20, TimeUnit.SECONDS)).isTrue();
            assertThat(persisted).isEmpty();
            replacement.start();
            TopicPartition partition = new TopicPartition(topic, 0);
            await().atMost(Duration.ofSeconds(35)).untilAsserted(() -> {
                assertThat(persisted).hasSize(10);
                var committed = admin.listConsumerGroupOffsets(group).partitionsToOffsetAndMetadata()
                        .get().get(partition);
                assertThat(committed).isNotNull();
                assertThat(committed.offset()).isEqualTo(10);
            });
        } finally {
            releaseFirst.countDown();
            replacement.stop();
            first.stop();
        }
    }

    private static KafkaMessageListenerContainer<String, String> listener(
            String topic, String group, BaseKafkaConsumer<String, String> processor) {
        var consumerFactory = new DefaultKafkaConsumerFactory<String, String>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, group,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10,
                ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 5000));
        ContainerProperties properties = new ContainerProperties(topic);
        properties.setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        properties.setMessageListener((AcknowledgingMessageListener<String, String>) (record, acknowledgment) -> {
            Message<String> message = MessageBuilder.withPayload(record.value())
                    .setHeader(KafkaHeaders.RECEIVED_PARTITION, record.partition())
                    .setHeader(KafkaHeaders.OFFSET, record.offset())
                    .setHeader(KafkaHeaders.ACKNOWLEDGMENT, acknowledgment)
                    .build();
            processor.execute(message);
        });
        return new KafkaMessageListenerContainer<>(consumerFactory, properties);
    }

    private static BaseKafkaConsumer<String, String> processor(Set<String> persisted,
            CountDownLatch firstStarted, CountDownLatch releaseFirst) {
        return new BaseKafkaConsumer<>("test") {
            private final ObjectReader reader = new ObjectMapper().readerFor(String.class);

            @Override protected ObjectReader getObjectReader() { return reader; }
            @Override protected Consumer<Throwable> onDeserializationError(Message<String> message) {
                return error -> { throw new AssertionError(error); };
            }
            @Override protected Mono<String> execute(String payload, Message<String> message, Map<String, Object> ctx) {
                return Mono.fromCallable(() -> {
                    if (firstStarted != null) {
                        firstStarted.countDown();
                        if (!releaseFirst.await(30, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("Blocked write timed out");
                        }
                    }
                    persisted.add(payload);
                    return payload;
                });
            }
        };
    }
}
