package it.gov.pagopa.notifier.service;

import it.gov.pagopa.notifier.configuration.DeleteProperties;
import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import it.gov.pagopa.notifier.connector.tpp.TppConnector;
import it.gov.pagopa.notifier.dto.RetryMessageRequestDTO;
import it.gov.pagopa.notifier.dto.TppDTO;
import it.gov.pagopa.notifier.enums.MessageState;
import it.gov.pagopa.notifier.model.Message;
import it.gov.pagopa.notifier.repository.MessageRepository;
import it.gov.pagopa.notifier.repository.MessageRepositoryCustomImpl;
import com.mongodb.client.result.UpdateResult;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient; // <--- NEW
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;

import static it.gov.pagopa.notifier.utils.TestUtils.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotifyServiceImplTest {

    private NotifyServiceImpl sendNotificationService;
    private static MockWebServer mockWebServer;

    @Mock
    private NotifyErrorProducerService errorProducerService;

    @Mock
    private MessageTemplateService messageTemplateService;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private DeleteProperties deleteProperties;

    @Mock
    private TppConnector tppConnector;

    @Mock
    private ReactiveMongoTemplate mongoTemplate;

    @BeforeAll
    static void setUpBefore() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.setDispatcher(new MyDispatcher());
        mockWebServer.start();
    }

    @BeforeEach
    void setUp() {
        sendNotificationService = new NotifyServiceImpl(
            errorProducerService,
            messageRepository,
            deleteProperties,
            messageTemplateService,
            WebClient.builder(),
            tppConnector
        );
    }

    @AfterAll
    static void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    void testDeleteMessages() {
        when(messageRepository.findAll()).thenReturn(Flux.just(MESSAGE));
        when(messageRepository.delete(any())).thenReturn(Mono.empty());
        when(messageRepository.count()).thenReturn(Mono.just(1L));
        StepVerifier.create(sendNotificationService.deleteMessages(DELETE_REQUEST_DTO))
            .expectNextCount(1)
            .verifyComplete();
    }

    @Test
    void testDeleteMessagesBatch() {
        when(messageRepository.findByMessageRegistrationDateBetween(any(), any())).thenReturn(Flux.just(MESSAGE));
        when(messageRepository.delete(any())).thenReturn(Mono.empty());
        when(messageRepository.count()).thenReturn(Mono.just(1L));
        when(deleteProperties.getBatchSize()).thenReturn(10);
        when(deleteProperties.getIntervalMs()).thenReturn(10);
        StepVerifier.create(sendNotificationService.cleanupOldMessages()).expectNextCount(1L).verifyComplete();
    }

    @Test
    void retryFailedMessage_claimsAndPublishesWithTppConfig() {
        String messageId = "message-retry";
        String entityId = TPP_DTO.getEntityId();
        Message claimedMessage = Message.builder()
                .messageId(messageId)
                .entityId(entityId)
                .messageState(MessageState.IN_PROCESS)
                .build();
        RetryMessageRequestDTO request = new RetryMessageRequestDTO(messageId, entityId);

        when(messageRepository.claimErroredMessage(messageId, entityId)).thenReturn(Mono.just(claimedMessage));
        when(tppConnector.getNotificationConfigByEntityId(entityId)).thenReturn(Mono.just(TPP_DTO));
        when(errorProducerService.publishManualRetry(claimedMessage, TPP_DTO)).thenReturn(Mono.empty());

        StepVerifier.create(sendNotificationService.retryFailedMessage(request)).verifyComplete();

        verify(messageRepository).claimErroredMessage(messageId, entityId);
        verify(tppConnector).getNotificationConfigByEntityId(entityId);
        verify(errorProducerService).publishManualRetry(claimedMessage, TPP_DTO);
        verify(messageRepository, never()).releaseManualRetryClaim(anyString(), anyString());
    }

    @Test
    void retryFailedMessage_releasesClaimWhenPublishFails() {
        String messageId = "message-retry";
        String entityId = TPP_DTO.getEntityId();
        Message claimedMessage = Message.builder()
                .messageId(messageId)
                .entityId(entityId)
                .messageState(MessageState.IN_PROCESS)
                .build();
        RetryMessageRequestDTO request = new RetryMessageRequestDTO(messageId, entityId);

        when(messageRepository.claimErroredMessage(messageId, entityId)).thenReturn(Mono.just(claimedMessage));
        when(tppConnector.getNotificationConfigByEntityId(entityId)).thenReturn(Mono.just(TPP_DTO));
        when(errorProducerService.publishManualRetry(claimedMessage, TPP_DTO))
                .thenReturn(Mono.error(new IllegalStateException("Kafka unavailable")));
        when(messageRepository.releaseManualRetryClaim(messageId, entityId)).thenReturn(Mono.just(true));

        StepVerifier.create(sendNotificationService.retryFailedMessage(request))
                .expectError()
                .verify();

        verify(messageRepository).releaseManualRetryClaim(messageId, entityId);
    }

    @Test
    void retryFailedMessage_returnsNotFoundWhenRecordDoesNotExist() {
        RetryMessageRequestDTO request = new RetryMessageRequestDTO("missing", "entity-1");
        when(messageRepository.claimErroredMessage("missing", "entity-1")).thenReturn(Mono.empty());
        when(messageRepository.existsByMessageIdAndEntityId("missing", "entity-1")).thenReturn(Mono.just(false));

        StepVerifier.create(sendNotificationService.retryFailedMessage(request))
                .expectErrorMatches(error -> error instanceof ClientExceptionWithBody exception
                        && exception.getHttpStatus() == HttpStatus.NOT_FOUND)
                .verify();
        verifyNoInteractions(tppConnector, errorProducerService);
    }

    @Test
    void retryFailedMessage_returnsConflictWhenRecordIsNotError() {
        RetryMessageRequestDTO request = new RetryMessageRequestDTO("sent", "entity-1");
        when(messageRepository.claimErroredMessage("sent", "entity-1")).thenReturn(Mono.empty());
        when(messageRepository.existsByMessageIdAndEntityId("sent", "entity-1")).thenReturn(Mono.just(true));

        StepVerifier.create(sendNotificationService.retryFailedMessage(request))
                .expectErrorMatches(error -> error instanceof ClientExceptionWithBody exception
                        && exception.getHttpStatus() == HttpStatus.CONFLICT)
                .verify();
        verifyNoInteractions(tppConnector, errorProducerService);
    }

    @Test
    void retryFailedMessage_releasesClaimWhenTppConfigIsMissing() {
        String messageId = "message-retry";
        String entityId = TPP_DTO.getEntityId();
        Message claimed = manualRetryMessage(messageId, entityId);
        when(messageRepository.claimErroredMessage(messageId, entityId)).thenReturn(Mono.just(claimed));
        when(tppConnector.getNotificationConfigByEntityId(entityId)).thenReturn(Mono.empty());
        when(messageRepository.releaseManualRetryClaim(messageId, entityId)).thenReturn(Mono.just(true));

        StepVerifier.create(sendNotificationService.retryFailedMessage(new RetryMessageRequestDTO(messageId, entityId)))
                .expectErrorMatches(error -> error instanceof ClientExceptionWithBody exception
                        && exception.getHttpStatus() == HttpStatus.BAD_GATEWAY)
                .verify();
        verify(messageRepository).releaseManualRetryClaim(messageId, entityId);
        verifyNoInteractions(errorProducerService);
    }

    @Test
    void retryFailedMessage_releasesClaimWhenTppConfigDoesNotMatchEntity() {
        String messageId = "message-retry";
        String entityId = "entity-1";
        Message claimed = manualRetryMessage(messageId, entityId);
        TppDTO wrongTpp = TppDTO.builder().entityId("different-entity").build();
        when(messageRepository.claimErroredMessage(messageId, entityId)).thenReturn(Mono.just(claimed));
        when(tppConnector.getNotificationConfigByEntityId(entityId)).thenReturn(Mono.just(wrongTpp));
        when(messageRepository.releaseManualRetryClaim(messageId, entityId)).thenReturn(Mono.just(true));

        StepVerifier.create(sendNotificationService.retryFailedMessage(new RetryMessageRequestDTO(messageId, entityId)))
                .expectErrorMatches(error -> error instanceof ClientExceptionWithBody exception
                        && exception.getHttpStatus() == HttpStatus.BAD_GATEWAY)
                .verify();
        verify(messageRepository).releaseManualRetryClaim(messageId, entityId);
        verifyNoInteractions(errorProducerService);
    }

    @Test
    void retryFailedMessage_propagatesErrorWhenClaimCannotBeReleased() {
        String messageId = "message-retry";
        String entityId = TPP_DTO.getEntityId();
        Message claimed = manualRetryMessage(messageId, entityId);
        when(messageRepository.claimErroredMessage(messageId, entityId)).thenReturn(Mono.just(claimed));
        when(tppConnector.getNotificationConfigByEntityId(entityId)).thenReturn(Mono.just(TPP_DTO));
        when(errorProducerService.publishManualRetry(claimed, TPP_DTO))
                .thenReturn(Mono.error(new IllegalStateException("Kafka unavailable")));
        when(messageRepository.releaseManualRetryClaim(messageId, entityId))
                .thenReturn(Mono.error(new IllegalStateException("Mongo unavailable")));

        StepVerifier.create(sendNotificationService.retryFailedMessage(new RetryMessageRequestDTO(messageId, entityId)))
                .expectErrorMatches(error -> error instanceof ClientExceptionWithBody exception
                        && exception.getHttpStatus() == HttpStatus.SERVICE_UNAVAILABLE)
                .verify();
    }

    private static Message manualRetryMessage(String messageId, String entityId) {
        return Message.builder()
                .messageId(messageId)
                .entityId(entityId)
                .messageState(MessageState.IN_PROCESS)
                .build();
    }

    @Test
    void testSendMessage_Success() {
        TPP_DTO.setAuthenticationUrl(mockWebServer.url(AUTHENTICATION_URL).toString());
        TPP_DTO.setMessageUrl(mockWebServer.url(MESSAGE_URL).toString());

        when(messageTemplateService.renderTemplate(any(), any()))
            .thenReturn(Mono.just("{\"mocked\":\"json_body\"}"));

        when(messageRepository.save(any())).thenReturn(Mono.just(MESSAGE));

        sendNotificationService.sendNotify(MESSAGE, TPP_DTO, RETRY).block();

        verify(messageTemplateService, times(1)).renderTemplate(any(), any()); // <--- Check chiamata template
        verify(messageRepository, times(1)).save(any());
    }

    @Test
    void testSendMessage_TokenFailure() {
        TPP_DTO.setAuthenticationUrl(mockWebServer.url("/fail").toString()); // URL che fallisce
        TPP_DTO.setMessageUrl(mockWebServer.url(MESSAGE_URL).toString());

        when(errorProducerService.enqueueNotify(any(), any(), anyLong())).thenReturn(Mono.just("Error"));
        sendNotificationService.sendNotify(MESSAGE, TPP_DTO, RETRY).block();

        verify(errorProducerService, times(1)).enqueueNotify(any(), any(), anyLong());
    }

    @Test
    void testSendMessage_SaveFail() {
        TPP_DTO.setAuthenticationUrl(mockWebServer.url(AUTHENTICATION_URL).toString());
        TPP_DTO.setMessageUrl(mockWebServer.url(MESSAGE_URL).toString());

        when(messageTemplateService.renderTemplate(any(), any()))
            .thenReturn(Mono.just("{\"mocked\":\"json_body\"}"));

        Mockito.when(messageRepository.save(any()))
            .thenReturn(Mono.error(new RuntimeException("Mocked save error")));
        when(errorProducerService.enqueueNotify(any(), any(), anyLong())).thenReturn(Mono.just("enqueued"));

        sendNotificationService.sendNotify(MESSAGE, TPP_DTO, RETRY).block();

        verify(messageTemplateService, times(1)).renderTemplate(any(), any());
        verify(messageRepository, times(1)).save(any());
        verify(errorProducerService).enqueueNotify(any(), any(), anyLong());
    }

    @Test
    void testSaveFailureAndRetryPublicationFailurePropagates() {
        TPP_DTO.setAuthenticationUrl(mockWebServer.url(AUTHENTICATION_URL).toString());
        TPP_DTO.setMessageUrl(mockWebServer.url(MESSAGE_URL).toString());
        when(messageTemplateService.renderTemplate(any(), any()))
            .thenReturn(Mono.just("{\"mocked\":\"json_body\"}"));
        when(messageRepository.save(any())).thenReturn(Mono.error(new RuntimeException("DB unavailable")));
        when(errorProducerService.enqueueNotify(any(), any(), anyLong()))
            .thenReturn(Mono.error(new IllegalStateException("Retry not published")));

        StepVerifier.create(sendNotificationService.sendNotify(MESSAGE, TPP_DTO, RETRY))
            .expectError(IllegalStateException.class)
            .verify();
    }

    @Test
    void testSendMessage_ToUrlFailure() {
        TPP_DTO.setAuthenticationUrl(mockWebServer.url(AUTHENTICATION_URL).toString());
        TPP_DTO.setMessageUrl(mockWebServer.url("/fail").toString());

        when(messageTemplateService.renderTemplate(any(), any()))
            .thenReturn(Mono.just("{\"mocked\":\"json_body\"}"));

        when(errorProducerService.enqueueNotify(any(), any(), anyLong())).thenReturn(Mono.just("Error"));

        sendNotificationService.sendNotify(MESSAGE, TPP_DTO, RETRY).block();

        verify(messageTemplateService, times(1)).renderTemplate(any(), any());
        verify(errorProducerService, times(1)).enqueueNotify(any(), any(), anyLong());
    }

    @Test
    void testSendMessage_TemplateError() {
        TPP_DTO.setAuthenticationUrl(mockWebServer.url(AUTHENTICATION_URL).toString());

        when(messageTemplateService.renderTemplate(any(), any()))
            .thenReturn(Mono.error(new RuntimeException("Template error")));

        when(errorProducerService.enqueueNotify(any(), any(), anyLong())).thenReturn(Mono.just("Error"));

        sendNotificationService.sendNotify(MESSAGE, TPP_DTO, RETRY).block();

        verify(errorProducerService, times(1)).enqueueNotify(any(), any(), anyLong());
        verify(messageRepository, never()).save(any());
    }

    @Test
    void emptyTokenResponse_RequiresConfirmedRetry() {
        TPP_DTO.setAuthenticationUrl(mockWebServer.url("/empty-token").toString());
        when(errorProducerService.enqueueNotify(any(), any(), anyLong()))
            .thenReturn(Mono.error(new IllegalStateException("Retry publication failed")));

        StepVerifier.create(sendNotificationService.sendNotify(MESSAGE, TPP_DTO, RETRY))
            .expectError(IllegalStateException.class)
            .verify();
        verify(errorProducerService).enqueueNotify(any(), any(), anyLong());
        verify(messageRepository, never()).save(any());
    }

    @Test
    void emptyTemplate_RequiresConfirmedRetry() {
        TPP_DTO.setAuthenticationUrl(mockWebServer.url(AUTHENTICATION_URL).toString());
        when(messageTemplateService.renderTemplate(any(), any())).thenReturn(Mono.empty());
        when(errorProducerService.enqueueNotify(any(), any(), anyLong()))
            .thenReturn(Mono.error(new IllegalStateException("Retry publication failed")));

        StepVerifier.create(sendNotificationService.sendNotify(MESSAGE, TPP_DTO, RETRY))
            .expectError(IllegalStateException.class)
            .verify();
        verify(errorProducerService).enqueueNotify(any(), any(), anyLong());
        verify(messageRepository, never()).save(any());
    }

    @Test
    void messageRepositoryCustom_claimReturnsUpdatedMessage() {
        MessageRepositoryCustomImpl repository = new MessageRepositoryCustomImpl(mongoTemplate);
        Message claimed = manualRetryMessage("message-1", "entity-1");
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class),
                any(FindAndModifyOptions.class), eq(Message.class))).thenReturn(Mono.just(claimed));

        StepVerifier.create(repository.claimErroredMessage("message-1", "entity-1"))
                .expectNext(claimed)
                .verifyComplete();
    }

    @Test
    void messageRepositoryCustom_releaseReturnsTrueWhenUpdated() {
        MessageRepositoryCustomImpl repository = new MessageRepositoryCustomImpl(mongoTemplate);
        UpdateResult updateResult = Mockito.mock(UpdateResult.class);
        when(updateResult.getModifiedCount()).thenReturn(1L);
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(Message.class)))
                .thenReturn(Mono.just(updateResult));

        StepVerifier.create(repository.releaseManualRetryClaim("message-1", "entity-1"))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void messageRepositoryCustom_releaseReturnsFalseWhenNotUpdated() {
        MessageRepositoryCustomImpl repository = new MessageRepositoryCustomImpl(mongoTemplate);
        UpdateResult updateResult = Mockito.mock(UpdateResult.class);
        when(updateResult.getModifiedCount()).thenReturn(0L);
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(Message.class)))
                .thenReturn(Mono.just(updateResult));

        StepVerifier.create(repository.releaseManualRetryClaim("message-1", "entity-1"))
                .expectNext(false)
                .verifyComplete();
    }

    static class MyDispatcher extends Dispatcher {
        @NotNull
        @Override
        public MockResponse dispatch(RecordedRequest request) {
            assert request.getPath() != null;
            if (request.getPath().equals("/empty-token")) {
                return new MockResponse().setResponseCode(204);
            } else if (request.getPath().equals(AUTHENTICATION_URL)) {
                return new MockResponse()
                    .setBody("{\"access_token\":\"accessToken\", \"token_type\":\"Bearer\", \"expires_in\":3600}")
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