package it.gov.pagopa.notifier.service;

import it.gov.pagopa.notifier.dto.MessageDTO;
import it.gov.pagopa.notifier.dto.mapper.MessageMapperDTOToObject;
import it.gov.pagopa.notifier.dto.TokenDTO;
import it.gov.pagopa.notifier.faker.MessageDTOFaker;
import it.gov.pagopa.notifier.faker.MessageFaker;
import it.gov.pagopa.notifier.faker.TokenDTOFaker;
import it.gov.pagopa.notifier.model.Message;
import it.gov.pagopa.notifier.repository.MessageRepository;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SendNotificationServiceImplTest {

    private static final String MESSAGE_URL = "/message";
    private static final String AUTHENTICATION_URL = "/auth";
    private static final String ENTITY_ID = "entity-id";
    private static final long RETRY = 1L;
    private static final String CLIENT_SECRET = "client_secret";
    private static final String CLIENT_ID = "client_id";
    private static final String GRANT_TYPE = "grant_type";
    private static final String TENANT_ID = "tenant_id";
    private static final String BEARER_TOKEN = "Bearer ";

    private static final MessageDTO MESSAGE_DTO = MessageDTOFaker.mockInstance();
    private static final Message MESSAGE = MessageFaker.mockInstance();
    private static final TokenDTO TOKEN_DTO = TokenDTOFaker.mockInstance();

    private SendNotificationServiceImpl sendNotificationService;
    private MockWebServer mockWebServer;

    @Mock
    private NotifyErrorProducerService errorProducerService;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private MessageMapperDTOToObject mapperDTOToObject;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        sendNotificationService = new SendNotificationServiceImpl(
                errorProducerService,
                messageRepository,
                mapperDTOToObject,
                CLIENT_SECRET,
                CLIENT_ID,
                GRANT_TYPE,
                TENANT_ID);
    }

    @AfterEach
    void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    void testSendMessage_Success() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"access_token\":\"accessToken\"}")
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        mockWebServer.enqueue(new MockResponse()
                .setBody("Message sent successfully")
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE));

        when(mapperDTOToObject.map(any(MessageDTO.class), any(String.class))).thenReturn(MESSAGE);
        when(messageRepository.save(any())).thenReturn(Mono.just(MESSAGE));

        sendNotificationService.sendNotification(MESSAGE_DTO, mockWebServer.url(MESSAGE_URL).toString(),
                mockWebServer.url(AUTHENTICATION_URL).toString(), ENTITY_ID, RETRY).block();

        verifyRequests();
        verify(messageRepository, times(1)).save(any());
    }

    @Test
    void testSendMessage_TokenFailure() {
        when(errorProducerService.enqueueNotify(any(),any(),any(),any(),anyLong())).thenReturn(Mono.just("Error"));
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error"));

        sendNotificationService.sendNotification(MESSAGE_DTO, mockWebServer.url(MESSAGE_URL).toString(),
                mockWebServer.url(AUTHENTICATION_URL).toString(), ENTITY_ID, RETRY).block();

        verify(errorProducerService, times(1)).enqueueNotify(any(), any(), any(), any(), anyLong());
    }

    @Test
    void testSendMessage_ToUrlFailure() {
        when(errorProducerService.enqueueNotify(any(),any(),any(),any(),anyLong())).thenReturn(Mono.just("Error"));

        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"access_token\":\"accessToken\"}")
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error"));

        sendNotificationService.sendNotification(MESSAGE_DTO, mockWebServer.url(MESSAGE_URL).toString(),
                mockWebServer.url(AUTHENTICATION_URL).toString(), ENTITY_ID, RETRY).block();

        verify(errorProducerService, times(1)).enqueueNotify(any(), any(), any(), any(), anyLong());
    }

    @Test
    void testSendMessageWithRetry_Success() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"access_token\":\"accessToken\"}")
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        mockWebServer.enqueue(new MockResponse()
                .setBody("Message sent successfully")
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE));

        when(mapperDTOToObject.map(any(MessageDTO.class), any(String.class))).thenReturn(MESSAGE);
        when(messageRepository.save(any())).thenReturn(Mono.just(MESSAGE));

        sendNotificationService.sendNotification(MESSAGE_DTO, mockWebServer.url(MESSAGE_URL).toString(),
                mockWebServer.url(AUTHENTICATION_URL).toString(), ENTITY_ID, RETRY).block();

        verifyRequests();
        verify(messageRepository, times(1)).save(any());
    }

    private void verifyRequests() throws InterruptedException {
        RecordedRequest authRequest = mockWebServer.takeRequest();
        Assertions.assertEquals(AUTHENTICATION_URL, authRequest.getPath());
        Assertions.assertEquals("POST", authRequest.getMethod());

        RecordedRequest messageRequest = mockWebServer.takeRequest();
        Assertions.assertEquals(MESSAGE_URL,messageRequest.getPath());
        Assertions.assertEquals("POST", messageRequest.getMethod());
        Assertions.assertEquals(messageRequest.getHeader(HttpHeaders.AUTHORIZATION), BEARER_TOKEN + TOKEN_DTO.getAccessToken());
    }
}
