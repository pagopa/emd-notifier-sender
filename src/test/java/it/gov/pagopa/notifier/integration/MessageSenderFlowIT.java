package it.gov.pagopa.notifier.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.notifier.dto.MessageDTO;
import it.gov.pagopa.notifier.dto.TokenSection;
import it.gov.pagopa.notifier.dto.TppDTO;
import it.gov.pagopa.notifier.dto.TokenDTO;
import it.gov.pagopa.notifier.enums.AuthenticationType;
import it.gov.pagopa.notifier.enums.Channel;
import it.gov.pagopa.notifier.enums.MessageState;
import it.gov.pagopa.notifier.enums.WorkflowType;
import it.gov.pagopa.notifier.model.Message;
import it.gov.pagopa.notifier.repository.MessageRepository;
import java.time.Instant;
import java.time.Period;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.mockserver.model.MediaType;
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
import java.util.*;
import java.util.stream.Collectors;

import static it.gov.pagopa.notifier.constants.NotifierSenderConstants.MessageHeader.ERROR_MSG_HEADER_RETRY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.JsonBody.json;

/**
 * Integration test for the complete message processing flow.
 */
@TestPropertySource(properties = "logging.level.it.gov.pagopa=DEBUG")
public class MessageSenderFlowIT extends BaseIT {

  private static final Logger log = LoggerFactory.getLogger(MessageSenderFlowIT.class);

  @Container
  static MockServerContainer mockServer = new MockServerContainer(
      DockerImageName.parse("mockserver/mockserver:latest")
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
  private static final String TEST_MESSAGE_ID = "MSG001";

  @DynamicPropertySource
  static void registerMockServerProperties(DynamicPropertyRegistry registry) {
    String mockServerUrl = "http://" + mockServer.getHost() + ":" + mockServer.getServerPort();
    registry.add("rest-client.citizen.baseUrl", () -> mockServerUrl);
    registry.add("rest-client.tpp.baseUrl", () -> mockServerUrl);
  }

  @BeforeEach
  void setUp() {
    mockServerClient = new MockServerClient(
        mockServer.getHost(),
        mockServer.getServerPort()
    );
    mockServerClient.reset();

    // Clear messages before each test
    messageRepository.deleteAll().block();

    // Wait that consumers are ready
    try {
      Thread.sleep(2000);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @Test
  void shouldProcessMessageSuccessfully_WithMockedExternalServices() throws Exception {
    // Setup mock responses for external services
    setupCitizenConnectorMock(TEST_FISCAL_CODE, List.of(TEST_TPP_ID));
    setupTppConnectorMock(List.of(TEST_TPP_ID));
    setupTokenMock();
    setupMessageUrlMock();

    MessageDTO messageDTO = createTestMessageDTO(TEST_MESSAGE_ID, TEST_FISCAL_CODE, Channel.SEND, WorkflowType.ANALOG);
    long retryCount = 0L;

    sendMessageToKafka(messageDTO, retryCount);

    // Wait for message processing
    await().atMost(Duration.ofSeconds(20))
        .pollInterval(Duration.ofMillis(500))
        .untilAsserted(() -> {
          List<Message> savedMessages = messageRepository
              .findAll()
              .filter(msg -> TEST_MESSAGE_ID.equals(msg.getMessageId()))
              .collectList()
              .block();

          log.info("Current saved messages: {}", savedMessages);
          assertThat(savedMessages).isNotEmpty();

          Message sentMessage = savedMessages.stream()
              .filter(msg -> msg.getMessageState() == MessageState.SENT)
              .findFirst()
              .orElse(null);

          assertThat(sentMessage).isNotNull();
          assertThat(sentMessage.getMessageId()).isEqualTo(TEST_MESSAGE_ID);
          assertThat(sentMessage.getRecipientId()).isEqualTo(TEST_FISCAL_CODE);
          assertThat(sentMessage.getChannel()).isEqualTo(Channel.SEND);
          assertThat(sentMessage.getSenderDescription()).isEqualTo("Test Sender");
        });

    // Verify external service calls
    mockServerClient.verify(
        request()
            .withPath("/emd/citizen/list/" + TEST_FISCAL_CODE + "/enabled/tpp")
            .withMethod("GET")
    );

    mockServerClient.verify(
        request()
            .withPath("/emd/tpp/list")
            .withMethod("POST")
    );

    mockServerClient.verify(
        request()
            .withPath("/auth/token")
            .withMethod("POST")
    );

    mockServerClient.verify(
        request()
            .withPath("/tpp/messages")
            .withMethod("POST")
    );
  }

  @Test
  void shouldHandleNoCitizensConsents() throws Exception {
    // No consents from citizen
    setupCitizenConnectorMock(TEST_FISCAL_CODE, Collections.emptyList());

    MessageDTO messageDTO = createTestMessageDTO(TEST_MESSAGE_ID, TEST_FISCAL_CODE, Channel.SEND, WorkflowType.ANALOG);
    long retryCount = 0L;

    sendMessageToKafka(messageDTO, retryCount);

    // Wait for citizen service call
    await().atMost(Duration.ofSeconds(15))
        .pollInterval(Duration.ofMillis(500))
        .untilAsserted(() -> {
          mockServerClient.verify(
              request()
                  .withPath("/emd/citizen/list/" + TEST_FISCAL_CODE + "/enabled/tpp")
                  .withMethod("GET"),
              org.mockserver.verify.VerificationTimes.atLeast(1)
          );
        });

    // Give some time to ensure no processing happened
    Thread.sleep(2000);

    // Verify that no messages were saved
    List<Message> savedMessages = messageRepository
        .findAll()
        .filter(msg -> TEST_MESSAGE_ID.equals(msg.getMessageId()))
        .collectList()
        .block();

    assertThat(savedMessages).isEmpty();

    // Verify that TPP service was never called
    mockServerClient.verify(
        request().withPath("/emd/tpp/list"),
        org.mockserver.verify.VerificationTimes.exactly(0)
    );
  }

  @Test
  void shouldRetryOnExternalServiceFailure() throws Exception {
    // Simulate temporary service failure
    mockServerClient
        .when(request()
            .withPath("/emd/citizen/list/" + TEST_FISCAL_CODE + "/enabled/tpp")
            .withMethod("GET"))
        .respond(response()
            .withStatusCode(500)
            .withBody("Internal Server Error"));

    MessageDTO messageDTO = createTestMessageDTO(TEST_MESSAGE_ID, TEST_FISCAL_CODE, Channel.SEND, WorkflowType.ANALOG);
    long retryCount = 0L;

    sendMessageToKafka(messageDTO, retryCount);

    // Verify retry attempts
    await().atMost(Duration.ofSeconds(15))
        .pollInterval(Duration.ofMillis(500))
        .untilAsserted(() -> {
          mockServerClient.verify(
              request()
                  .withPath("/emd/citizen/list/" + TEST_FISCAL_CODE + "/enabled/tpp")
                  .withMethod("GET"),
              org.mockserver.verify.VerificationTimes.atLeast(2)
          );
        });

    // Verify message is not marked as SENT
    List<Message> savedMessages = messageRepository
        .findAll()
        .filter(msg -> TEST_MESSAGE_ID.equals(msg.getMessageId()))
        .collectList()
        .block();

    if (savedMessages != null && !savedMessages.isEmpty()) {
      assertThat(savedMessages).noneMatch(msg -> msg.getMessageState() == MessageState.SENT);
      assertThat(savedMessages).allMatch(msg ->
          msg.getMessageState() == MessageState.ERROR
      );
    }
  }

  @Test
  void shouldHandleMultipleTppIds() throws Exception {
    // Test with multiple TPP IDs
    List<String> tppIds = List.of("TPP001", "TPP002", "TPP003");

    setupCitizenConnectorMock(TEST_FISCAL_CODE, tppIds);
    setupTppConnectorMock(tppIds);
    setupTokenMock();
    setupMessageUrlMock();

    MessageDTO messageDTO = createTestMessageDTO(TEST_MESSAGE_ID, TEST_FISCAL_CODE, Channel.SEND, WorkflowType.ANALOG);
    long retryCount = 0L;

    sendMessageToKafka(messageDTO, retryCount);

    await().atMost(Duration.ofSeconds(20))
        .pollInterval(Duration.ofMillis(500))
        .untilAsserted(() -> {
          List<Message> savedMessages = messageRepository
              .findAll()
              .filter(msg -> TEST_MESSAGE_ID.equals(msg.getMessageId()))
              .collectList()
              .block();

          assertThat(savedMessages).isNotEmpty();
          assertThat(savedMessages).anyMatch(msg -> msg.getMessageState() == MessageState.SENT);
        });

    mockServerClient.verify(
        request()
            .withPath("/emd/tpp/list")
            .withMethod("POST")
            .withBody(json(Map.of("ids", tppIds)))
    );
  }

  // ============ MOCK SETUP METHODS ============

  private void setupCitizenConnectorMock(String fiscalCode, List<String> tppIds) {
    mockServerClient
        .when(request()
            .withPath("/emd/citizen/list/" + fiscalCode + "/enabled/tpp")
            .withMethod("GET"))
        .respond(response()
            .withStatusCode(200)
            .withContentType(MediaType.APPLICATION_JSON)
            .withBody(json(tppIds)));
  }

  private void setupTppConnectorMock(List<String> tppIds) throws Exception {
    List<TppDTO> tppDTOs = createMockTppDTOs(tppIds);

    mockServerClient
        .when(request()
            .withPath("/emd/tpp/list")
            .withMethod("POST")
            .withBody(json(Map.of("ids", tppIds))))
        .respond(response()
            .withStatusCode(200)
            .withContentType(MediaType.APPLICATION_JSON)
            .withBody(objectMapper.writeValueAsString(tppDTOs)));
  }

  private void setupTokenMock() throws Exception {
    TokenDTO tokenDTO = TokenDTO.builder()
        .accessToken("mock-access-token-12345")
        .tokenType("Bearer")
        .expiresIn(3600)
        .build();

    mockServerClient
        .when(request()
            .withPath("/auth/token")
            .withMethod("POST"))
        .respond(response()
            .withStatusCode(200)
            .withContentType(MediaType.APPLICATION_JSON)
            .withBody(objectMapper.writeValueAsString(tokenDTO)));
  }

  private void setupMessageUrlMock() {
    mockServerClient
        .when(request()
            .withPath("/tpp/messages")
            .withMethod("POST"))
        .respond(response()
            .withStatusCode(200)
            .withContentType(MediaType.APPLICATION_JSON)
            .withBody(json(Map.of("status", "success", "messageId", TEST_MESSAGE_ID))));
  }

  // ============ HELPER METHODS ============

  private List<TppDTO> createMockTppDTOs(List<String> tppIds) {
    return tppIds.stream()
        .map(tppId -> {
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
              .authenticationUrl("http://" + mockServer.getHost() + ":" +
                  mockServer.getServerPort() + "/auth/token")
              .messageUrl("http://" + mockServer.getHost() + ":" +
                  mockServer.getServerPort() + "/tpp/messages")
              .tokenSection(TokenSection.builder()
                  .contentType("application/x-www-form-urlencoded")
                  .bodyAdditionalProperties(tokenProps)
                  .build())
              .state(true)
              .messageTemplate("""
                {
                  "messageId": "${messageId?json_string}",
                  "recipientId": "${recipientId?json_string}",
                  "triggerDateTimeUTC": "${triggerDateTimeUTC?json_string}",
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
        })
        .collect(Collectors.toList());
  }

  private MessageDTO createTestMessageDTO(String messageId, String recipientId, Channel channel, WorkflowType workflowType) {
    return MessageDTO.builder()
        .messageId(messageId)
        .recipientId(recipientId)
        .channel(channel)
        .triggerDateTime(Instant.now().toString())
        .senderDescription("Test Sender")
        .messageUrl("https://example.com/message/" + messageId)
        .originId("ORIGIN_" + messageId)
        .content("Test content for " + messageId)
        .title("Test notes")
        .associatedPayment(false)
        .idPsp("PSP_TEST")
        .analogSchedulingDate(Instant.now().plus(Period.ofDays(5)).toString())
        .workflowType(workflowType)
        .build();
  }

  /**
   * Send a message to Kafka using StreamBridge
   */
  private void sendMessageToKafka(MessageDTO messageDTO, Long retryCount) throws Exception {
    log.info("Sending message to Kafka: messageId={}, retry={}", messageDTO.getMessageId(), retryCount);

    String messageJson = objectMapper.writeValueAsString(messageDTO);

    boolean sent = streamBridge.send(
        "messageSender-out-0",
        MessageBuilder
            .withPayload(messageJson)
            .setHeader(ERROR_MSG_HEADER_RETRY, retryCount)
            .build()
    );

    if (!sent) {
      throw new RuntimeException("Failed to send message to Kafka");
    }

    log.info("Message sent successfully to Kafka");

    // Wait message to be consumed
    Thread.sleep(500);
  }
}