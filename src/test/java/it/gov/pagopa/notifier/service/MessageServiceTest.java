package it.gov.pagopa.notifier.service;

import it.gov.pagopa.common.reactive.kafka.exception.UncommittableError;
import it.gov.pagopa.notifier.connector.citizen.CitizenConnectorImpl;
import it.gov.pagopa.notifier.connector.tpp.TppConnectorImpl;
import it.gov.pagopa.notifier.custom.CitizenInvocationException;
import it.gov.pagopa.notifier.custom.TppInvocationException;
import it.gov.pagopa.notifier.enums.MessageState;
import it.gov.pagopa.notifier.model.Message;
import it.gov.pagopa.notifier.model.mapper.MessageMapperDTOToObject;
import it.gov.pagopa.notifier.repository.MessageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.dao.DuplicateKeyException;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static it.gov.pagopa.notifier.utils.TestUtils.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@ContextConfiguration(classes = MessageServiceImpl.class)
class MessageServiceTest {

    @MockitoBean
    CitizenConnectorImpl citizenService;
    @MockitoBean
    TppConnectorImpl tppService;
    @MockitoBean
    MessageCoreProducerServiceImpl messageCoreProducerService;
    @MockitoBean
    NotifyServiceImpl sendNotificationService;
    @MockitoBean
    MessageRepository messageRepository;

    @MockitoBean
    MessageMapperDTOToObject messageMapperDTOToObject;

    @Autowired
    MessageServiceImpl messageService;

    @Test
    void sendMessage_PersistError_NeverCompletesSuccessfully()  {
        // L'insert fallito deve arrivare al consumer come errore non committabile.
        Mockito.when(citizenService.getCitizenConsentsEnabled(any()))
                .thenReturn(Mono.just(TPP_ID_STRING_LIST));

        Mockito.when(tppService.filterEnabledList(any()))
                .thenReturn(Mono.just(TPP_DTO_LIST));

        Mockito.when(messageMapperDTOToObject.map(any(), any(), any(), any()))
                .thenReturn(MESSAGE);

        Mockito.when(messageRepository.insert(Mockito.<Message>any()))
                .thenReturn(Mono.<Message>error(new RuntimeException("Mocked persist error")));

       assertThrows(UncommittableError.class, () -> messageService.processMessage(MESSAGE_DTO,0).block());
       verify(messageCoreProducerService,times(0)).enqueueMessage(any(),anyLong());
       verify(sendNotificationService,times(0)).sendNotify(MESSAGE,TPP_DTO,0);

    }

    @Test
    void sendMessage_PartialInsertFailure_DoesNotCommitWholeMessage() {
        var secondTpp = Mockito.mock(it.gov.pagopa.notifier.dto.TppDTO.class);
        Mockito.when(secondTpp.getIdPsp()).thenReturn("otherPsp");
        Mockito.when(secondTpp.getEntityId()).thenReturn("otherEntity");
        Mockito.when(citizenService.getCitizenConsentsEnabled(any()))
                .thenReturn(Mono.just(TPP_ID_STRING_LIST));
        Mockito.when(tppService.filterEnabledList(any()))
                .thenReturn(Mono.just(List.of(TPP_DTO, secondTpp)));
        Mockito.when(messageMapperDTOToObject.map(any(), any(), any(), any()))
                .thenReturn(MESSAGE);
        Mockito.when(sendNotificationService.sendNotify(any(), any(), anyLong()))
                .thenReturn(Mono.empty());
        AtomicInteger inserts = new AtomicInteger();
        Mockito.when(messageRepository.insert(Mockito.<Message>any()))
                .thenReturn(Mono.defer(() -> inserts.incrementAndGet() == 2
                        ? Mono.error(new IllegalStateException("Second insert failed"))
                        : Mono.just(MESSAGE)));

        assertThrows(UncommittableError.class,
                () -> messageService.processMessage(MESSAGE_DTO, 0).block());
        verify(messageCoreProducerService,times(0)).enqueueMessage(any(),anyLong());
    }

    @Test
    void sendMessage_Ok()  {
        Mockito.when(citizenService.getCitizenConsentsEnabled(any()))
                .thenReturn(Mono.just(TPP_ID_STRING_LIST));

        Mockito.when(tppService.filterEnabledList(any()))
                .thenReturn(Mono.just(TPP_DTO_LIST));

        Mockito.when(sendNotificationService.sendNotify(MESSAGE,TPP_DTO,0))
                .thenReturn(Mono.empty());

        Mockito.when(messageMapperDTOToObject.map(any(), any(), any(), any()))
                .thenReturn(MESSAGE);

        Mockito.when(messageRepository.insert(Mockito.<Message>any()))
                .thenReturn(Mono.just(MESSAGE));

        messageService.processMessage(MESSAGE_DTO,0).block();
        verify(messageCoreProducerService,times(0)).enqueueMessage(MESSAGE_DTO,0);
        verify(sendNotificationService,times(1)).sendNotify(MESSAGE,TPP_DTO,0);
    }

    @Test
    void sendMessage_Duplicate_SkippedOnDuplicateKey()  {
        // A completed message is safe to skip on Kafka redelivery.
        Mockito.when(citizenService.getCitizenConsentsEnabled(any()))
                .thenReturn(Mono.just(TPP_ID_STRING_LIST));

        Mockito.when(tppService.filterEnabledList(any()))
                .thenReturn(Mono.just(TPP_DTO_LIST));

        Mockito.when(messageMapperDTOToObject.map(any(), any(), any(), any()))
                .thenReturn(MESSAGE);

        Mockito.when(messageRepository.insert(Mockito.<Message>any()))
                .thenReturn(Mono.error(new DuplicateKeyException("duplicate natural key")));
        Message existing = Mockito.mock(Message.class);
        Mockito.when(existing.getMessageState()).thenReturn(MessageState.SENT);
        Mockito.when(messageRepository.findByMessageIdAndEntityId(any(), any()))
                .thenReturn(Mono.just(existing));

        messageService.processMessage(MESSAGE_DTO,0).block();

        verify(sendNotificationService,times(0)).sendNotify(any(),any(),anyLong());
        verify(messageCoreProducerService,times(0)).enqueueMessage(any(),anyLong());
    }

    @Test
    void sendMessage_DuplicateInProcess_ResumesNotification() {
        Mockito.when(citizenService.getCitizenConsentsEnabled(any())).thenReturn(Mono.just(TPP_ID_STRING_LIST));
        Mockito.when(tppService.filterEnabledList(any())).thenReturn(Mono.just(TPP_DTO_LIST));
        Mockito.when(messageMapperDTOToObject.map(any(), any(), any(), any())).thenReturn(MESSAGE);
        Mockito.when(messageRepository.insert(Mockito.<Message>any()))
                .thenReturn(Mono.error(new DuplicateKeyException("duplicate natural key")));
        Message existing = Mockito.mock(Message.class);
        Mockito.when(existing.getMessageState()).thenReturn(MessageState.IN_PROCESS);
        Mockito.when(messageRepository.findByMessageIdAndEntityId(any(), any()))
                .thenReturn(Mono.just(existing));
        Mockito.when(sendNotificationService.sendNotify(existing, TPP_DTO, 0)).thenReturn(Mono.empty());

        messageService.processMessage(MESSAGE_DTO, 0).block();

        verify(sendNotificationService).sendNotify(existing, TPP_DTO, 0);
    }

    @Test
    void sendMessage_UnrelatedDuplicateWithoutMatchingRecord_DoesNotAcknowledge() {
        Mockito.when(citizenService.getCitizenConsentsEnabled(any())).thenReturn(Mono.just(TPP_ID_STRING_LIST));
        Mockito.when(tppService.filterEnabledList(any())).thenReturn(Mono.just(TPP_DTO_LIST));
        Mockito.when(messageMapperDTOToObject.map(any(), any(), any(), any())).thenReturn(MESSAGE);
        Mockito.when(messageRepository.insert(Mockito.<Message>any()))
                .thenReturn(Mono.error(new DuplicateKeyException("other index")));
        Mockito.when(messageRepository.findByMessageIdAndEntityId(any(), any()))
                .thenReturn(Mono.empty());

        assertThrows(UncommittableError.class, () -> messageService.processMessage(MESSAGE_DTO, 0).block());
        verify(sendNotificationService, times(0)).sendNotify(any(), any(), anyLong());
    }

    @Test
    void sendMessage_NoChannelEnabled_Case_NoConsents()  {
        Mockito.when(citizenService.getCitizenConsentsEnabled(any()))
                .thenReturn(Mono.just(Collections.emptyList()));

        messageService.processMessage(MESSAGE_DTO,0).block();
        verify(messageCoreProducerService,times(0)).enqueueMessage(MESSAGE_DTO,0);

    }

    @Test
    void sendMessage_NoChannelEnabled_Case_NoChannels()  {
        Mockito.when(citizenService.getCitizenConsentsEnabled(any()))
                .thenReturn(Mono.just(TPP_ID_STRING_LIST));

        Mockito.when(tppService.filterEnabledList(any()))
                .thenReturn(Mono.just(Collections.emptyList()));

        messageService.processMessage(MESSAGE_DTO,0).block();
        verify(messageCoreProducerService,times(0)).enqueueMessage(MESSAGE_DTO,0);

    }

    @Test
    void sendMessage_Ko_CitizenException()  {

        Mockito.when(citizenService.getCitizenConsentsEnabled(any()))
                .thenReturn(Mono.error(new CitizenInvocationException()));

        Mockito.when(messageCoreProducerService.enqueueMessage(any(),anyLong()))
                .thenReturn(Mono.empty());

        messageService.processMessage(MESSAGE_DTO,0).block();
        verify(messageCoreProducerService,times(1)).enqueueMessage(MESSAGE_DTO,1);

    }

    @Test
    void sendMessage_Ko_CitizenRetryPublicationFailed() {
        Mockito.when(citizenService.getCitizenConsentsEnabled(any()))
                .thenReturn(Mono.error(new CitizenInvocationException()));
        Mockito.when(messageCoreProducerService.enqueueMessage(any(),anyLong()))
                .thenReturn(Mono.error(new UncommittableError("Retry not published")));

        assertThrows(UncommittableError.class,
                () -> messageService.processMessage(MESSAGE_DTO,0).block());
    }

    @Test
    void sendMessage_Ko_TppException()  {

        Mockito.when(citizenService.getCitizenConsentsEnabled(any()))
                .thenReturn(Mono.just(TPP_ID_STRING_LIST));

        Mockito.when(tppService.filterEnabledList(any()))
                .thenReturn(Mono.error(new TppInvocationException()));

        Mockito.when(messageCoreProducerService.enqueueMessage(any(),anyLong()))
                .thenReturn(Mono.empty());

        messageService.processMessage(MESSAGE_DTO,0).block();
        verify(messageCoreProducerService,times(1)).enqueueMessage(MESSAGE_DTO,1);

    }
}
