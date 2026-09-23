package it.gov.pagopa.notifier.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.Document;
import it.gov.pagopa.notifier.dto.MessageDTO;
import it.gov.pagopa.notifier.dto.TokenDTO;
import it.gov.pagopa.notifier.dto.TokenSection;
import it.gov.pagopa.notifier.dto.TppDTO;
import it.gov.pagopa.notifier.enums.AuthenticationType;
import it.gov.pagopa.notifier.enums.Channel;
import it.gov.pagopa.notifier.enums.MessageState;
import it.gov.pagopa.notifier.enums.WorkflowType;
import it.gov.pagopa.notifier.model.Message;
import it.gov.pagopa.notifier.repository.MessageRepository;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.MediaType;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static it.gov.pagopa.notifier.constants.NotifierSenderConstants.MessageHeader.ERROR_MSG_HEADER_RETRY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.JsonBody.json;

/**
 * Integration tests validating the reliability fixes:
 * <ul>
 *   <li>Idempotency: a redelivered message (Kafka at-least-once) must not duplicate
 *       the persisted document nor the TPP notification.</li>
 *   <li>Exhausted retries: a message that exhausts its retries must be persisted in state ERROR.</li>
 * </ul>
 */
@TestPropertySource(properties = {
    "logging.level.it.gov.pagopa=DEBUG",
    // Backoff azzerato + max-retry=0 per rendere i test del flusso di errore rapidi e deterministici.
    "app.retry.max-retry=0",
    "app.retry.initial-delay-seconds=0",
    "app.retry.max-delay-seconds=0"
})
class IdempotencyAndDlqFlowIT extends BaseIT {

  @Container
  static MockServerContainer mockServer = new MockServerContainer(
      // Pin alla versione del client mockserver-client-java (5.15.0) nel pom:
      // il tag "latest" punta a una major incompatibile (7.x).
      DockerImageName.parse("mockserver/mockserver:mockserver-5.15.0")
  );

  private MockServerClient mockServerClient;

  @Autowired
  private MessageRepository messageRepository;
  @Autowired
  private ReactiveMongoTemplate mongoTemplate;
  @Autowired
  private ObjectMapper objectMapper;
  @Autowired
  private StreamBridge streamBridge;

  private static final String TEST_FISCAL_CODE = "RSSMRA80A01H501U";
  private static final String TEST_TPP_ID = "TPP001";
  private static final String TEST_MESSAGE_ID = "MSG-IDEMPOTENT-001";
  private static final String MESSAGE_TOPIC = "test-courtesy-message";
  private static final String MESSAGE_GROUP = "test-courtesy-consumer-group";

  @DynamicPropertySource
  static void registerMockServerProperties(DynamicPropertyRegistry registry) {
    String mockServerUrl = "http://" + mockServer.getHost() + ":" + mockServer.getServerPort();
    registry.add("rest-client.citizen.baseUrl", () -> mockServerUrl);
    registry.add("rest-client.tpp.baseUrl", () -> mockServerUrl);
  }

  @BeforeEach
  void setUp() throws InterruptedException {
    mockServerClient = new MockServerClient(mockServer.getHost(), mockServer.getServerPort());
    mockServerClient.reset();
    messageRepository.deleteAll().block();
    // MongoDB Testcontainers does not inherit the Cosmos collection indexes.
    mongoTemplate.indexOps(Message.class).ensureIndex(new Index()
        .on("messageId", Sort.Direction.ASC).on("entityId", Sort.Direction.ASC)
        .unique().named("messageId_1_entityId_1")).block();
    assertThat(mongoTemplate.indexOps(Message.class).getIndexInfo().collectList().block())
        .anySatisfy(index -> {
          assertThat(index.getName()).isEqualTo("messageId_1_entityId_1");
          assertThat(index.isUnique()).isTrue();
          assertThat(index.getIndexFields()).hasSize(2);
        });
    Thread.sleep(2000); // attesa che i consumer siano pronti
  }

  @Test
  void redelivery_isIdempotent_singleDocumentAndSingleTppCall() throws Exception {
    setupCitizenConnectorMock(TEST_FISCAL_CODE, List.of(TEST_TPP_ID));
    setupTppConnectorMock(List.of(TEST_TPP_ID));
    setupTokenMock();
    setupMessageUrlMock();

    MessageDTO messageDTO = createTestMessageDTO(TEST_MESSAGE_ID, TEST_FISCAL_CODE);

    // Prima consegna: il messaggio viene elaborato e inviato al TPP.
    sendMessageToKafka(messageDTO, 0L);

    await().atMost(Duration.ofSeconds(20))
        .pollInterval(Duration.ofMillis(500))
        .untilAsserted(() -> {
          List<Message> saved = messageRepository.findAll()
              .filter(m -> TEST_MESSAGE_ID.equals(m.getMessageId()))
              .collectList().block();
          assertThat(saved).isNotNull();
          assertThat(saved).anyMatch(m -> m.getMessageState() == MessageState.SENT);
        });

    // Il TPP è stato chiamato una volta.
    mockServerClient.verify(
        request().withPath("/tpp/messages").withMethod("POST"),
        org.mockserver.verify.VerificationTimes.exactly(1));

    // Seconda consegna dello STESSO messaggio (simula redelivery Kafka at-least-once).
    sendMessageToKafka(messageDTO, 0L);
    Thread.sleep(5000); // diamo tempo all'eventuale (errato) ri-processamento

    // IDEMPOTENZA: nessuna seconda notifica al TPP...
    mockServerClient.verify(
        request().withPath("/tpp/messages").withMethod("POST"),
        org.mockserver.verify.VerificationTimes.exactly(1));

    // ...e un solo documento persistito per la chiave naturale (vincolo unique compound messageId-entityId).
    List<Message> finalDocs = messageRepository.findAll()
        .filter(m -> TEST_MESSAGE_ID.equals(m.getMessageId()))
        .collectList().block();
    assertThat(finalDocs).hasSize(1);
    assertThat(finalDocs.get(0).getMessageState()).isEqualTo(MessageState.SENT);
    assertThat(finalDocs.get(0).getEntityId())
        .isEqualTo("ENTITY_" + TEST_TPP_ID);
  }

  @Test
  void compoundIndex_rejectsSameMessageAndEntity_butAllowsAnotherEntity() {
    messageRepository.insert(Message.builder().messageId("MSG-INDEX-001")
        .entityId("ENTITY_A").build()).block();
    messageRepository.insert(Message.builder().messageId("MSG-INDEX-001")
        .entityId("ENTITY_B").build()).block();

    assertThatThrownBy(() -> messageRepository.insert(Message.builder()
        .messageId("MSG-INDEX-001").entityId("ENTITY_A").build()).block())
        .isInstanceOf(DuplicateKeyException.class);
    assertThat(messageRepository.findAll().filter(message ->
        "MSG-INDEX-001".equals(message.getMessageId())).collectList().block()).hasSize(2);
  }

  @Test
  void insertFailure_doesNotCommitOffset_andRecoversAfterMongoIsRestored() throws Exception {
    String messageId = "MSG-MONGO-RECOVERY-001";
    setupCitizenConnectorMock(TEST_FISCAL_CODE, List.of(TEST_TPP_ID));
    setupTppConnectorMock(List.of(TEST_TPP_ID));
    setupTokenMock();
    setupMessageUrlMock();

    // A Mongo validator rejects only this message's inserts (not reads or other messages).
    // collMod acts on the actual collection used by the repository, not a guessed name.
    String collection = mongoTemplate.getCollectionName(Message.class);
    mongoTemplate.executeCommand(new Document("collMod", collection)
        .append("validator", new Document("messageId", new Document("$ne", messageId)))
        .append("validationAction", "error")).block();

    try (AdminClient admin = AdminClient.create(Map.of(
        AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers()))) {
      TopicPartition partition = new TopicPartition(MESSAGE_TOPIC, 0);
      long offsetBefore = committedOffset(admin, partition);
      try {
        sendMessageToKafka(createTestMessageDTO(messageId, TEST_FISCAL_CODE), 0L);
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
            mockServerClient.verify(request().withPath("/emd/tpp/list").withMethod("POST"),
                org.mockserver.verify.VerificationTimes.atLeast(1)));

        // Allow the commit buffer to flush: even after a DB rejection, no ack is legal.
        Thread.sleep(1500);
        assertThat(committedOffset(admin, partition)).isEqualTo(offsetBefore);
        assertThat(messageRepository.findByMessageIdAndEntityId(messageId, "ENTITY_" + TEST_TPP_ID).block())
            .isNull();
        mockServerClient.verify(request().withPath("/tpp/messages").withMethod("POST"),
            org.mockserver.verify.VerificationTimes.exactly(0));
      } finally {
        mongoTemplate.executeCommand(new Document("collMod", collection)
            .append("validator", new Document())).block();
      }

      await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(500))
          .untilAsserted(() -> {
            Message saved = messageRepository.findByMessageIdAndEntityId(
                messageId, "ENTITY_" + TEST_TPP_ID).block();
            assertThat(saved).isNotNull();
            assertThat(saved.getMessageState()).isEqualTo(MessageState.SENT);
            assertThat(committedOffset(admin, partition)).isGreaterThan(offsetBefore);
          });
      mockServerClient.verify(request().withPath("/tpp/messages").withMethod("POST"),
          org.mockserver.verify.VerificationTimes.exactly(1));
    }
  }

  private long committedOffset(AdminClient admin, TopicPartition partition) throws Exception {
    var offset = admin.listConsumerGroupOffsets(MESSAGE_GROUP).partitionsToOffsetAndMetadata()
        .get().get(partition);
    return offset == null ? -1 : offset.offset();
  }

  @Test
  void exhaustedRetries_persistsErrorState() throws Exception {
    setupCitizenConnectorMock(TEST_FISCAL_CODE, List.of(TEST_TPP_ID));
    setupTppConnectorMock(List.of(TEST_TPP_ID));
    setupTokenMock();
    // Il TPP risponde sempre 500 -> la notifica fallisce e, con max-retry=0, viene persistita in stato ERROR.
    mockServerClient
        .when(request().withPath("/tpp/messages").withMethod("POST"))
        .respond(response().withStatusCode(500).withBody("error"));

    MessageDTO messageDTO = createTestMessageDTO("MSG-ERROR-001", TEST_FISCAL_CODE);
    sendMessageToKafka(messageDTO, 0L);

    // Il messaggio risulta persistito in stato ERROR.
    await().atMost(Duration.ofSeconds(20))
        .pollInterval(Duration.ofMillis(500))
        .untilAsserted(() -> {
          List<Message> saved = messageRepository.findAll()
              .filter(m -> "MSG-ERROR-001".equals(m.getMessageId()))
              .collectList().block();
          assertThat(saved).isNotNull();
          assertThat(saved).anyMatch(m -> m.getMessageState() == MessageState.ERROR);
        });
  }


  // ============ MOCK SETUP ============

  private void setupCitizenConnectorMock(String fiscalCode, List<String> tppIds) {
    mockServerClient
        .when(request().withPath("/emd/citizen/list/" + fiscalCode + "/enabled/tpp").withMethod("GET"))
        .respond(response().withStatusCode(200).withContentType(MediaType.APPLICATION_JSON).withBody(json(tppIds)));
  }

  private void setupTppConnectorMock(List<String> tppIds) throws Exception {
    List<TppDTO> tppDTOs = tppIds.stream().map(this::buildTpp).toList();
    mockServerClient
        .when(request().withPath("/emd/tpp/list").withMethod("POST").withBody(json(Map.of("ids", tppIds))))
        .respond(response().withStatusCode(200).withContentType(MediaType.APPLICATION_JSON)
            .withBody(objectMapper.writeValueAsString(tppDTOs)));
  }

  private void setupTokenMock() throws Exception {
    TokenDTO tokenDTO = TokenDTO.builder().accessToken("mock-token").tokenType("Bearer").expiresIn(3600).build();
    mockServerClient
        .when(request().withPath("/auth/token").withMethod("POST"))
        .respond(response().withStatusCode(200).withContentType(MediaType.APPLICATION_JSON)
            .withBody(objectMapper.writeValueAsString(tokenDTO)));
  }

  private void setupMessageUrlMock() {
    mockServerClient
        .when(request().withPath("/tpp/messages").withMethod("POST"))
        .respond(response().withStatusCode(200).withContentType(MediaType.APPLICATION_JSON)
            .withBody(json(Map.of("status", "success"))));
  }

  private TppDTO buildTpp(String tppId) {
    Map<String, String> tokenProps = new HashMap<>();
    tokenProps.put("grant_type", "client_credentials");
    tokenProps.put("client_id", "client_" + tppId);
    tokenProps.put("client_secret", "secret_" + tppId);
    return TppDTO.builder()
        .tppId(tppId)
        .entityId("ENTITY_" + tppId)
        .idPsp("PSP_" + tppId)
        .businessName("Business " + tppId)
        .authenticationType(AuthenticationType.OAUTH2)
        .authenticationUrl("http://" + mockServer.getHost() + ":" + mockServer.getServerPort() + "/auth/token")
        .messageUrl("http://" + mockServer.getHost() + ":" + mockServer.getServerPort() + "/tpp/messages")
        .tokenSection(TokenSection.builder()
            .contentType("application/x-www-form-urlencoded")
            .bodyAdditionalProperties(tokenProps)
            .build())
        .state(true)
        .messageTemplate("""
            {
              "messageId": "${messageId?json_string}",
              "recipientId": "${recipientId?json_string}",
              "triggerDateTime": "${triggerDateTime?json_string}",
              "messageUrl": "${messageUrl?json_string}",
              "idPsp": "${idPsp?json_string}",
              "senderDescription": "${(senderDescription! == '')?then('', senderDescription?json_string)}",
              "originId": ${originId???then('"' + originId?json_string + '"', 'null')},
              "title": ${title???then('"' + title?json_string + '"', 'null')},
              "content": ${content???then('"' + content?json_string + '"', 'null')},
              "analogSchedulingDate": ${analogSchedulingDate???then('"' + analogSchedulingDate?json_string + '"', 'null')},
              "workflowType": ${workflowType???then('"' + workflowType?json_string + '"', 'null')},
              "associatedPayment": ${associatedPayment???then(associatedPayment?c, 'null')}
            }
            """)
        .build();
  }

  private MessageDTO createTestMessageDTO(String messageId, String recipientId) {
    return MessageDTO.builder()
        .messageId(messageId)
        .recipientId(recipientId)
        .channel(Channel.SEND)
        .triggerDateTime(Instant.now().toString())
        .senderDescription("Test Sender")
        .messageUrl("https://example.com/message/" + messageId)
        .originId("ORIGIN_" + messageId)
        .content("Test content for " + messageId)
        .title("Test notes")
        .associatedPayment(false)
        .idPsp("PSP_TEST")
        .analogSchedulingDate(Instant.now().plus(Period.ofDays(5)).toString())
        .workflowType(WorkflowType.ANALOG)
        .build();
  }

  private void sendMessageToKafka(MessageDTO messageDTO, Long retryCount) throws Exception {
    String messageJson = objectMapper.writeValueAsString(messageDTO);
    boolean sent = streamBridge.send(
        "messageSender-out-0",
        MessageBuilder.withPayload(messageJson).setHeader(ERROR_MSG_HEADER_RETRY, retryCount).build());
    if (!sent) {
      throw new IllegalStateException("Failed to send message to Kafka");
    }
    Thread.sleep(500);
  }
}

