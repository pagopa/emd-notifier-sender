package it.gov.pagopa.notifier.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@Testcontainers
@SpringBootTest(webEnvironment = RANDOM_PORT)
abstract class BaseIT {

  private static final Logger log = LoggerFactory.getLogger(BaseIT.class);

  @Container
  protected static final MongoDBContainer mongo =
      new MongoDBContainer("mongo:8.0.15-noble");

  @Container
  protected static final ConfluentKafkaContainer kafka =
      new ConfluentKafkaContainer(
          DockerImageName.parse("confluentinc/cp-kafka:7.6.0")
              .asCompatibleSubstituteFor("apache/kafka"))
          .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true")
          .withExposedPorts(9092);

  @LocalServerPort
  protected int port;

  @DynamicPropertySource
  static void setProperties(DynamicPropertyRegistry registry) {
    log.info("Configuring Spring properties using Mongo replica set {} and Kafka on {}",
        mongo.getReplicaSetUrl(),
        kafka.getBootstrapServers());

    // MongoDB
    registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
    registry.add("spring.data.mongodb.database", () -> "test-db");


    // Kafka - Basic configuration
    registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);

    // Disable SASL for testcontainers
    registry.add("spring.cloud.stream.kafka.binder.configuration.sasl.mechanism", () -> "");
    registry.add("spring.cloud.stream.kafka.binder.configuration.security.protocol", () -> "PLAINTEXT");

    // Binder configuration - overrides the one in application.yml
    registry.add("spring.cloud.stream.kafka.binder.brokers", kafka::getBootstrapServers);
    registry.add("spring.cloud.stream.kafka.binder.auto-create-topics", () -> "true");

    // Disable custom defined binders
    registry.add("spring.cloud.stream.binders.kafka-message-core-in.environment.spring.cloud.stream.kafka.binder.brokers",
        kafka::getBootstrapServers);
    registry.add("spring.cloud.stream.binders.kafka-message-core-out.environment.spring.cloud.stream.kafka.binder.brokers",
        kafka::getBootstrapServers);
    registry.add("spring.cloud.stream.binders.kafka-notify-error-in.environment.spring.cloud.stream.kafka.binder.brokers",
        kafka::getBootstrapServers);
    registry.add("spring.cloud.stream.binders.kafka-notify-error-out.environment.spring.cloud.stream.kafka.binder.brokers",
        kafka::getBootstrapServers);

    // Disable SASL for all binders
    registry.add("spring.cloud.stream.binders.kafka-message-core-in.environment.spring.cloud.stream.kafka.binder.configuration.sasl.jaas.config", () -> "");
    registry.add("spring.cloud.stream.binders.kafka-message-core-out.environment.spring.cloud.stream.kafka.binder.configuration.sasl.jaas.config", () -> "");
    registry.add("spring.cloud.stream.binders.kafka-notify-error-in.environment.spring.cloud.stream.kafka.binder.configuration.sasl.jaas.config", () -> "");
    registry.add("spring.cloud.stream.binders.kafka-notify-error-out.environment.spring.cloud.stream.kafka.binder.configuration.sasl.jaas.config", () -> "");

    // Spring Cloud Stream - Bindings for courtesy message (consumer)
    registry.add("spring.cloud.stream.bindings.consumerMessage-in-0.destination",
        () -> "test-courtesy-message");
    registry.add("spring.cloud.stream.bindings.consumerMessage-in-0.group",
        () -> "test-courtesy-consumer-group");
    registry.add("spring.cloud.stream.bindings.consumerMessage-in-0.binder",
        () -> "kafka-message-core-in");

    // Spring Cloud Stream - Bindings for courtesy message (producer)
    registry.add("spring.cloud.stream.bindings.messageSender-out-0.destination",
        () -> "test-courtesy-message");
    registry.add("spring.cloud.stream.bindings.messageSender-out-0.binder",
        () -> "kafka-message-core-out");

    // Spring Cloud Stream - Bindings for error queue (consumer)
    registry.add("spring.cloud.stream.bindings.consumerNotify-in-0.destination",
        () -> "test-notify-error");
    registry.add("spring.cloud.stream.bindings.consumerNotify-in-0.group",
        () -> "test-error-consumer-group");
    registry.add("spring.cloud.stream.bindings.consumerNotify-in-0.binder",
        () -> "kafka-notify-error-in");

    // Spring Cloud Stream - Bindings for error queue (producer)
    registry.add("spring.cloud.stream.bindings.notifySender-out-0.destination",
        () -> "test-notify-error");
    registry.add("spring.cloud.stream.bindings.notifySender-out-0.binder",
        () -> "kafka-notify-error-out");

  }
}