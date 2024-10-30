package it.gov.pagopa.notifier.service;


import it.gov.pagopa.notifier.dto.MessageDTO;
import it.gov.pagopa.notifier.dto.MessageMapperDTOToObject;
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

    private SendNotificationServiceImpl sendMessageService;
    private MockWebServer mockWebServer;

    @Mock
    private QueueMessageProducerService errorProducerService;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private MessageMapperDTOToObject mapperDTOToObject;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

         sendMessageService = new SendNotificationServiceImpl
                (errorProducerService, messageRepository, mapperDTOToObject, "client_secret", "client_id", "grant_type", "tenant_id");
    }

    @AfterEach
    void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    void testSendMessage_Success() throws InterruptedException {

        MessageDTO messageDTO = MessageDTOFaker.mockInstance();
        Message message = MessageFaker.mockInstance();
        String messageUrl = "/message";
        String authenticationUrl = "/auth";
        String entityId = "entity-id";

        TokenDTO tokenDTO = TokenDTOFaker.mockInstance();


        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"access_token\":\"accessToken\"}")
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));


        mockWebServer.enqueue(new MockResponse()
                .setBody("Message sent successfully")
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE));

        when(mapperDTOToObject.map(any(MessageDTO.class), any(String.class))).thenReturn(message);
        when(messageRepository.save(any())).thenReturn(Mono.just(message));

        sendMessageService.sendMessage(messageDTO, mockWebServer.url(messageUrl).toString(), mockWebServer.url(authenticationUrl).toString(), entityId).block();

        RecordedRequest authRequest = mockWebServer.takeRequest();
        Assertions.assertEquals(authRequest.getPath(),authenticationUrl);
        Assertions.assertEquals("POST", authRequest.getMethod());

        RecordedRequest messageRequest = mockWebServer.takeRequest();
        Assertions.assertEquals(messageRequest.getPath(),messageUrl);
        Assertions.assertEquals("POST", messageRequest.getMethod());
        Assertions.assertEquals(messageRequest.getHeader(HttpHeaders.AUTHORIZATION),"Bearer " + tokenDTO.getAccessToken());

        verify(messageRepository, times(1)).save(any());
    }

    @Test
    void testSendMessage_TokenFailure() {
        MessageDTO messageDTO = MessageDTOFaker.mockInstance();
        String messageUrl = "/message";
        String authenticationUrl = "/auth";
        String entityId = "entity-id";

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error"));

        sendMessageService.sendMessage(messageDTO, mockWebServer.url(messageUrl).toString(), mockWebServer.url(authenticationUrl).toString(), entityId).block();

        verify(errorProducerService, times(1)).enqueueMessage(any(), any(), any(), any());
    }

    @Test
    void testSendMessage_ToUrlFailure() {
        MessageDTO messageDTO = MessageDTOFaker.mockInstance();
        String messageUrl = "/message";
        String authenticationUrl = "/auth";
        String entityId = "entity-id";

        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"access_token\":\"accessToken\"}")
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error"));

        sendMessageService.sendMessage(messageDTO, mockWebServer.url(messageUrl).toString(), mockWebServer.url(authenticationUrl).toString(), entityId).block();

        verify(errorProducerService, times(1)).enqueueMessage(any(), any(), any(), any());
    }

    @Test
    void testSendMessageWithRetry_Success() throws InterruptedException {

        MessageDTO messageDTO = MessageDTOFaker.mockInstance();
        Message message = MessageFaker.mockInstance();
        String messageUrl = "/message";
        String authenticationUrl = "/auth";
        String entityId = "entity-id";
        long retry = 1L;

        TokenDTO tokenDTO = TokenDTOFaker.mockInstance();


        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"access_token\":\"accessToken\"}")
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));


        mockWebServer.enqueue(new MockResponse()
                .setBody("Message sent successfully")
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE));

        when(mapperDTOToObject.map(any(MessageDTO.class), any(String.class))).thenReturn(message);
        when(messageRepository.save(any())).thenReturn(Mono.just(message));

        sendMessageService.sendMessage(messageDTO, mockWebServer.url(messageUrl).toString(), mockWebServer.url(authenticationUrl).toString(), entityId, retry).block();

        RecordedRequest authRequest = mockWebServer.takeRequest();
        Assertions.assertEquals(authRequest.getPath(),authenticationUrl);
        Assertions.assertEquals("POST", authRequest.getMethod());

        RecordedRequest messageRequest = mockWebServer.takeRequest();
        Assertions.assertEquals(messageRequest.getPath(),messageUrl);
        Assertions.assertEquals("POST", messageRequest.getMethod());
        Assertions.assertEquals(messageRequest.getHeader(HttpHeaders.AUTHORIZATION),"Bearer " + tokenDTO.getAccessToken());

        verify(messageRepository, times(1)).save(any());
    }

    @Test
    void testSendMessageWithRetry_TokenFailure() {
        MessageDTO messageDTO = MessageDTOFaker.mockInstance();
        String messageUrl = "/message";
        String authenticationUrl = "/auth";
        String entityId = "entity-id";
        long retry = 1L;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error"));

        sendMessageService.sendMessage(messageDTO, mockWebServer.url(messageUrl).toString(), mockWebServer.url(authenticationUrl).toString(), entityId,retry).block();

        verify(errorProducerService, times(1)).enqueueMessage(any(), any(), any(), any(),anyLong());
    }

    @Test
    void testSendMessageWithRetry_ToUrlFailure() {
        MessageDTO messageDTO = MessageDTOFaker.mockInstance();
        String messageUrl = "/message";
        String authenticationUrl = "/auth";
        String entityId = "entity-id";
        long retry = 1L;

        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"access_token\":\"accessToken\"}")
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error"));

        sendMessageService.sendMessage(messageDTO, mockWebServer.url(messageUrl).toString(), mockWebServer.url(authenticationUrl).toString(), entityId,retry).block();

        verify(errorProducerService, times(1)).enqueueMessage(any(), any(), any(), any(),anyLong());
    }
}
