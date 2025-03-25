package it.gov.pagopa.notifier.service;

import it.gov.pagopa.notifier.repository.MessageRepository;
import okhttp3.mockwebserver.*;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

import java.io.IOException;

import static it.gov.pagopa.notifier.utils.TestUtils.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotifyServiceImplTest {

    private  NotifyServiceImpl sendNotificationService;
    private static MockWebServer mockWebServer;

    @Mock
    private NotifyErrorProducerService errorProducerService;

    @Mock
    private MessageRepository messageRepository;


    @BeforeAll
    static void setUpBefore() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.setDispatcher(new MyDispatcher());
        mockWebServer.start();
    }

    @BeforeEach
    void setUp(){
        sendNotificationService = new NotifyServiceImpl(
                errorProducerService,
                messageRepository,
                "");
    }

    @AfterAll
    static void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    void testSendMessage_Success() {
        TPP_DTO.setAuthenticationUrl(mockWebServer.url(AUTHENTICATION_URL).toString());
        TPP_DTO.setMessageUrl(mockWebServer.url(MESSAGE_URL).toString());
        when(messageRepository.save(any())).thenReturn(Mono.just(MESSAGE));

        sendNotificationService.sendNotify(MESSAGE, TPP_DTO, RETRY).block();

        verify(messageRepository, times(1)).save(any());
    }



    @Test
    void testSendMessage_TokenFailure() {
        TPP_DTO.setAuthenticationUrl(mockWebServer.url("/fail").toString());
        TPP_DTO.setMessageUrl(mockWebServer.url(MESSAGE_URL).toString());
        when(errorProducerService.enqueueNotify(any(),any(),anyLong())).thenReturn(Mono.just("Error"));

        sendNotificationService.sendNotify(MESSAGE, TPP_DTO, RETRY).block();

        verify(errorProducerService, times(1)).enqueueNotify(any(), any(), anyLong());
    }
    @Test
    void testSendMessage_SaveFail(){
        TPP_DTO.setAuthenticationUrl(mockWebServer.url(AUTHENTICATION_URL).toString());
        TPP_DTO.setMessageUrl(mockWebServer.url(MESSAGE_URL).toString());
        Mockito.when(messageRepository.save(any()))
                .thenReturn(Mono.error(new RuntimeException("Mocked save error")));

        sendNotificationService.sendNotify(MESSAGE, TPP_DTO, RETRY).block();

        verify(messageRepository, times(1)).save(any());
    }
    @Test
    void testSendMessage_ToUrlFailure() {
        TPP_DTO.setAuthenticationUrl(mockWebServer.url(AUTHENTICATION_URL).toString());
        TPP_DTO.setMessageUrl(mockWebServer.url("/fail").toString());
        when(errorProducerService.enqueueNotify(any(),any(),anyLong())).thenReturn(Mono.just("Error"));

        sendNotificationService.sendNotify(MESSAGE,TPP_DTO,RETRY).block();

        verify(errorProducerService, times(1)).enqueueNotify(any(), any(), anyLong());
    }

    static class MyDispatcher extends Dispatcher {
        @NotNull
        @Override
        public MockResponse dispatch(RecordedRequest request){
            assert request.getPath() != null;
            if (request.getPath().equals(AUTHENTICATION_URL)) {
                return new MockResponse()
                        .setBody("{\"access_token\":\"accessToken\"}")
                        .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            } else if (request.getPath().equals(MESSAGE_URL)) {
                return new MockResponse()
                        .setBody("Message sent successfully")
                        .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE);
            }
            return new MockResponse()
                    .setResponseCode(500)
                    .setBody("Internal Server Error");
        }
    }
}
