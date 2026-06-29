package it.gov.pagopa.notifier.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static it.gov.pagopa.notifier.constants.NotifierSenderConstants.MessageHeader.ERROR_MSG_HEADER_RETRY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.JsonBody.json;

/**
 * Integration tests validating the reliability fixes:
 * <ul>
 *   <li>Idempotency: a redelivered message (Kafka at-least-once) must not duplicate
 *       the persisted document nor the TPP notification.</li>
 *   <li>Dead Letter Queue: a message that exhausts its retries must land on the DLQ topic.</li>
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

  private static final Logger log = LoggerFactory.getLogger(IdempotencyAndDlqFlowIT.class);

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
  private ObjectMapper objectMapper;
  @Autowired
  private StreamBridge streamBridge;

  private static final String TEST_FISCAL_CODE = "RSSMRA80A01H501U";
  private static final String TEST_TPP_ID = "TPP001";
  private static final String TEST_MESSAGE_ID = "MSG-IDEMPOTENT-001";

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

    // ...e un solo documento persistito per la chiave naturale (id deterministico = upsert).
    List<Message> finalDocs = messageRepository.findAll()
        .filter(m -> TEST_MESSAGE_ID.equals(m.getMessageId()))
        .collectList().block();
    assertThat(finalDocs).hasSize(1);
    assertThat(finalDocs.get(0).getMessageState()).isEqualTo(MessageState.SENT);
    assertThat(finalDocs.get(0).getId())
        .isEqualTo(TEST_MESSAGE_ID + ":" + "ENTITY_" + TEST_TPP_ID);
  }

  @Test
  void exhaustedRetries_routesToDlqTopic() throws Exception {
    setupCitizenConnectorMock(TEST_FISCAL_CODE, List.of(TEST_TPP_ID));
    setupTppConnectorMock(List.of(TEST_TPP_ID));
    setupTokenMock();
    // Il TPP risponde sempre 500 -> la notifica fallisce e, con max-retry=0, finisce subito in DLQ.
    mockServerClient
        .when(request().withPath("/tpp/messages").withMethod("POST"))
        .respond(response().withStatusCode(500).withBody("error"));

    try (KafkaConsumer<String, String> dlqConsumer = createDlqConsumer()) {
      dlqConsumer.subscribe(Collections.singletonList("test-notify-dlq"));

      MessageDTO messageDTO = createTestMessageDTO("MSG-DLQ-001", TEST_FISCAL_CODE);
      sendMessageToKafka(messageDTO, 0L);

      ConsumerRecord<String, String> dlqRecord = pollForDlqRecord(dlqConsumer);

      assertThat(dlqRecord).as("a record must be routed to the DLQ topic").isNotNull();
      assertThat(dlqRecord.value()).contains("MSG-DLQ-001");
    }

    // Il messaggio risulta persistito in stato ERROR.
    await().atMost(Duration.ofSeconds(20))
        .pollInterval(Duration.ofMillis(500))
        .untilAsserted(() -> {
          List<Message> saved = messageRepository.findAll()
              .filter(m -> "MSG-DLQ-001".equals(m.getMessageId()))
              .collectList().block();
          assertThat(saved).isNotNull();
          assertThat(saved).anyMatch(m -> m.getMessageState() == MessageState.ERROR);
        });
  }

  // ============ KAFKA DLQ CONSUMER ============

  private KafkaConsumer<String, String> createDlqConsumer() {
    Properties props = new Properties();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-dlq-verifier-" + System.currentTimeMillis());
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    return new KafkaConsumer<>(props);
  }

  private ConsumerRecord<String, String> pollForDlqRecord(KafkaConsumer<String, String> consumer) {
    long deadline = System.currentTimeMillis() + Duration.ofSeconds(30).toMillis();
    while (System.currentTimeMillis() < deadline) {
      ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
      if (!records.isEmpty()) {
        return records.iterator().next();
      }
    }
    return null;
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

