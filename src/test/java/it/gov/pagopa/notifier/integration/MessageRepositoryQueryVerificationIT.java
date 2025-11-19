package it.gov.pagopa.notifier.integration;

import it.gov.pagopa.notifier.enums.Channel;
import it.gov.pagopa.notifier.enums.MessageState;
import it.gov.pagopa.notifier.model.Message;
import it.gov.pagopa.notifier.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Integration test verifying MongoDB queries for MessageRepository.
 *
 * <p>Uses MongoDB driver debug logging to inspect generated queries.
 * Check console output for actual query structure.</p>
 */
@TestPropertySource(properties = {
    "logging.level.org.springframework.data.mongodb.core.ReactiveMongoTemplate=DEBUG",
})
public class MessageRepositoryQueryVerificationIT extends BaseIT {
  private static final Logger log = LoggerFactory.getLogger(MessageRepositoryQueryVerificationIT.class);

  private static final String COLLECTION_NAME = "message";

  private static final String TEST_RECIPIENT_1 = "TESTCF00A00B000C";
  private static final String TEST_RECIPIENT_2 = "TESTCF00A00B000D";
  private static final String TEST_ENTITY_1 = "entity-tpp-001";
  private static final String TEST_ENTITY_2 = "entity-tpp-002";
  private static final String TEST_MESSAGE_ID_1 = "msg-001";
  private static final String TEST_MESSAGE_ID_2 = "msg-002";
  private static final String TEST_MESSAGE_ID_3 = "msg-003";

  @Autowired
  ReactiveMongoTemplate mongoTemplate;

  @Autowired
  MessageRepository repository;

  @BeforeEach
  void setup() {
    // Drop collection
    StepVerifier.create(
        mongoTemplate.dropCollection(COLLECTION_NAME)
            .onErrorResume(e -> Mono.empty())
    ).verifyComplete();

    String triggerDateTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

    Message message1 = Message.builder()
        .messageId(TEST_MESSAGE_ID_1)
        .recipientId(TEST_RECIPIENT_1)
        .entityId(TEST_ENTITY_1)
        .channel(Channel.SEND)
        .messageRegistrationDate(LocalDateTime.parse("2025-10-15T15:00:00").format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        .messageState(MessageState.IN_PROCESS)
        .triggerDateTime(triggerDateTime)
        .senderDescription("Test Sender 1")
        .messageUrl("https://example.com/message1")
        .originId("origin-001")
        .content("Test message content 1")
        .title("Test title 1")
        .associatedPayment(false)
        .idPsp("PSP-001")
        .build();

    Message message2 = Message.builder()
        .messageId(TEST_MESSAGE_ID_2)
        .recipientId(TEST_RECIPIENT_1)
        .entityId(TEST_ENTITY_2)
        .channel(Channel.SEND)
        .messageRegistrationDate(LocalDateTime.parse("2025-10-20T00:00:00").format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        .messageState(MessageState.SENT)
        .triggerDateTime(triggerDateTime)
        .senderDescription("Test Sender 2")
        .messageUrl("https://example.com/message2")
        .originId("origin-002")
        .content("Test message content 2")
        .title("Test title 2")
        .associatedPayment(true)
        .idPsp("PSP-002")
        .build();

    Message message3 = Message.builder()
        .messageId(TEST_MESSAGE_ID_3)
        .recipientId(TEST_RECIPIENT_2)
        .entityId(TEST_ENTITY_1)
        .channel(Channel.SEND)
        .messageRegistrationDate(LocalDateTime.parse("2025-10-25T12:00:00").format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        .messageState(MessageState.IN_PROCESS)
        .triggerDateTime(triggerDateTime)
        .senderDescription("Test Sender 3")
        .messageUrl("https://example.com/message3")
        .originId("origin-003")
        .content("Test message content 3")
        .title("Test title 3")
        .associatedPayment(false)
        .idPsp("PSP-003")
        .build();

    // Insert test data
    StepVerifier.create(
        mongoTemplate.save(message1, COLLECTION_NAME)
            .then(mongoTemplate.save(message2, COLLECTION_NAME))
            .then(mongoTemplate.save(message3, COLLECTION_NAME))
    ).expectNextCount(1).verifyComplete();
  }

  @Test
  void testFindByRecipientIdAndEntityId() {
    log.info("=== EXECUTING findByRecipientIdAndEntityId ===");

    StepVerifier.create(
            repository.findByRecipientIdAndEntityId(TEST_RECIPIENT_1, TEST_ENTITY_1)
        )
        .assertNext(message -> {
          log.info("Found message: {}", message);
          assert message.getRecipientId().equals(TEST_RECIPIENT_1);
          assert message.getEntityId().equals(TEST_ENTITY_1);
          assert message.getMessageId().equals(TEST_MESSAGE_ID_1);
          assert message.getMessageState().equals(MessageState.IN_PROCESS);
          assert message.getChannel().equals(Channel.SEND);
        })
        .verifyComplete();

    log.info("=== TEST COMPLETED - CHECK LOGS ABOVE FOR QUERY DETAILS ===");
  }

  @Test
  void testFindByRecipientIdAndEntityIdNotFound() {
    log.info("=== EXECUTING findByRecipientIdAndEntityId (not found) ===");

    StepVerifier.create(
            repository.findByRecipientIdAndEntityId("NOTFOUND", "NOTFOUND")
        )
        .verifyComplete();

    log.info("=== TEST COMPLETED ===");
  }

  @Test
  void testFindByRecipientIdAndEntityIdMultipleResults() {
    log.info("=== EXECUTING findByRecipientIdAndEntityId (multiple results) ===");

    // Add another message for same recipient and entity
    Message message4 = Message.builder()
        .messageId("msg-004")
        .recipientId(TEST_RECIPIENT_1)
        .entityId(TEST_ENTITY_1)
        .channel(Channel.SEND)
        .messageRegistrationDate(LocalDateTime.parse("2025-10-28T12:00:00").format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        .messageState(MessageState.SENT)
        .triggerDateTime(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        .senderDescription("Test Sender 4")
        .messageUrl("https://example.com/message4")
        .originId("origin-004")
        .content("Test message content 4")
        .title("Test title 4")
        .associatedPayment(true)
        .idPsp("PSP-004")
        .build();

    StepVerifier.create(
        mongoTemplate.save(message4, COLLECTION_NAME)
            .then(Mono.empty())
    ).verifyComplete();

    StepVerifier.create(
            repository.findByRecipientIdAndEntityId(TEST_RECIPIENT_1, TEST_ENTITY_1)
        )
        .expectNextCount(2)
        .verifyComplete();

    log.info("=== TEST COMPLETED - CHECK LOGS ABOVE FOR QUERY DETAILS ===");
  }

  @Test
  void testFindByMessageIdAndEntityId() {
    log.info("=== EXECUTING findByMessageIdAndEntityId ===");

    StepVerifier.create(
            repository.findByMessageIdAndEntityId(TEST_MESSAGE_ID_1, TEST_ENTITY_1)
        )
        .assertNext(message -> {
          log.info("Found message: {}", message);
          assert message.getMessageId().equals(TEST_MESSAGE_ID_1);
          assert message.getEntityId().equals(TEST_ENTITY_1);
          assert message.getRecipientId().equals(TEST_RECIPIENT_1);
          assert message.getSenderDescription().equals("Test Sender 1");
          assert message.getContent().equals("Test message content 1");
        })
        .verifyComplete();

    log.info("=== TEST COMPLETED - CHECK LOGS ABOVE FOR QUERY DETAILS ===");
  }

  @Test
  void testFindByMessageIdAndEntityIdNotFound() {
    log.info("=== EXECUTING findByMessageIdAndEntityId (not found) ===");

    StepVerifier.create(
            repository.findByMessageIdAndEntityId("NOTFOUND", TEST_ENTITY_1)
        )
        .verifyComplete();

    log.info("=== TEST COMPLETED ===");
  }

  @Test
  void testFindByMessageIdAndEntityIdWrongEntity() {
    log.info("=== EXECUTING findByMessageIdAndEntityId (wrong entity) ===");

    StepVerifier.create(
            repository.findByMessageIdAndEntityId(TEST_MESSAGE_ID_1, TEST_ENTITY_2)
        )
        .verifyComplete();

    log.info("=== TEST COMPLETED ===");
  }

  @Test
  void testFindByMessageRegistrationDateBetween() {
    log.info("=== EXECUTING findByMessageRegistrationDateBetween ===");

    StepVerifier.create(
            repository.findByMessageRegistrationDateBetween("2025-10-14", "2025-10-21")
        )
        .assertNext(message -> {
          log.info("Found message: {}", message);
          assert message.getMessageId().equals(TEST_MESSAGE_ID_1);
          assert message.getMessageRegistrationDate().equals("2025-10-15T15:00:00");
        })
        .assertNext(message -> {
          log.info("Found message: {}", message);
          assert message.getMessageId().equals(TEST_MESSAGE_ID_2);
          assert message.getMessageRegistrationDate().equals("2025-10-20T00:00:00");
        })
        .verifyComplete();

    log.info("=== TEST COMPLETED - CHECK LOGS ABOVE FOR QUERY DETAILS ===");
  }

  @Test
  void testFindByMessageRegistrationDateBetweenSingleResult() {
    log.info("=== EXECUTING findByMessageRegistrationDateBetween (single result) ===");

    StepVerifier.create(
            repository.findByMessageRegistrationDateBetween("2025-10-24T00:00:00", "2025-10-26T00:00:00")
        )
        .assertNext(message -> {
          log.info("Found message: {}", message);
          assert message.getMessageId().equals(TEST_MESSAGE_ID_3);
          assert message.getMessageRegistrationDate().equals("2025-10-25T12:00:00");
        })
        .verifyComplete();

    log.info("=== TEST COMPLETED ===");
  }

  @Test
  void testFindByMessageRegistrationDateBetweenNoResults() {
    log.info("=== EXECUTING findByMessageRegistrationDateBetween (no results) ===");

    StepVerifier.create(
            repository.findByMessageRegistrationDateBetween("2025-10-01T00:00:00", "2025-10-10T00:00:00")
        )
        .verifyComplete();

    log.info("=== TEST COMPLETED ===");
  }

  @Test
  void testFindByMessageRegistrationDateBetweenAllMessages() {
    log.info("=== EXECUTING findByMessageRegistrationDateBetween (all messages) ===");

    StepVerifier.create(
            repository.findByMessageRegistrationDateBetween("2025-10-01T00:00:00", "2025-10-31T00:00:00")
        )
        .expectNextCount(3)
        .verifyComplete();

    log.info("=== TEST COMPLETED - CHECK LOGS ABOVE FOR QUERY DETAILS ===");
  }

  @Test
  void testFindByMessageRegistrationDateBetweenExcludesBounds() {
    log.info("=== EXECUTING testFindByMessageRegistrationDateBetweenExcludesBounds (edge cases) ===");

    // Exact match on start date
    StepVerifier.create(
            repository.findByMessageRegistrationDateBetween("2025-10-15T15:00:00", "2025-10-15T15:00:00")
        )
        .expectNextCount(0)
        .verifyComplete();

    log.info("=== TEST COMPLETED ===");
  }

  @Test
  void testFindAll() {
    log.info("=== EXECUTING findAll ===");

    StepVerifier.create(
            repository.findAll()
        )
        .expectNextCount(3)
        .verifyComplete();

    log.info("=== TEST COMPLETED - CHECK LOGS ABOVE FOR QUERY DETAILS ===");
  }

  @Test
  void testCount() {
    log.info("=== EXECUTING count ===");

    StepVerifier.create(
            repository.count()
        )
        .assertNext(count -> {
          log.info("Total message count: {}", count);
          assert count == 3L;
        })
        .verifyComplete();

    log.info("=== TEST COMPLETED ===");
  }

  @Test
  void testDeleteMessage() {
    log.info("=== EXECUTING delete ===");

    StepVerifier.create(
            repository.findByMessageIdAndEntityId(TEST_MESSAGE_ID_1, TEST_ENTITY_1)
                .flatMap(repository::delete)
                .then(repository.count())
        )
        .assertNext(count -> {
          log.info("Remaining message count after delete: {}", count);
          assert count == 2L;
        })
        .verifyComplete();

    log.info("=== TEST COMPLETED ===");
  }
}