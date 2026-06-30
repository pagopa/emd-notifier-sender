package it.gov.pagopa.notifier.service;

import it.gov.pagopa.notifier.connector.citizen.CitizenConnectorImpl;
import it.gov.pagopa.notifier.connector.tpp.TppConnectorImpl;
import it.gov.pagopa.notifier.custom.CitizenInvocationException;
import it.gov.pagopa.notifier.custom.TppInvocationException;
import it.gov.pagopa.notifier.model.Message;
import it.gov.pagopa.notifier.model.mapper.MessageMapperDTOToObject;
import it.gov.pagopa.notifier.repository.MessageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;

import java.util.Collections;

import static it.gov.pagopa.notifier.utils.TestUtils.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@ContextConfiguration(classes = MessageServiceImpl.class)
class MessageServiceTest {

    @MockBean
    CitizenConnectorImpl citizenService;
    @MockBean
    TppConnectorImpl tppService;
    @MockBean
    MessageCoreProducerServiceImpl messageCoreProducerService;
    @MockBean
    NotifyServiceImpl sendNotificationService;
    @MockBean
    MessageRepository messageRepository;

    @MockBean
    MessageMapperDTOToObject messageMapperDTOToObject;

    @Autowired
    MessageServiceImpl messageService;

    @Test
    void sendMessage_PersistError_SkipsWithoutNotify()  {
        // Errore di persistenza generico (non duplicate): non notifichiamo e non ri-accodiamo;
        // il messaggio verrà ritentato dal flusso a monte.
        Mockito.when(citizenService.getCitizenConsentsEnabled(any()))
                .thenReturn(Mono.just(TPP_ID_STRING_LIST));

        Mockito.when(tppService.filterEnabledList(any()))
                .thenReturn(Mono.just(TPP_DTO_LIST));

        Mockito.when(messageMapperDTOToObject.map(any(), any(), any(), any()))
                .thenReturn(MESSAGE);

        Mockito.when(messageRepository.insert(Mockito.<Message>any()))
                .thenReturn(Mono.<Message>error(new RuntimeException("Mocked persist error")));

       messageService.processMessage(MESSAGE_DTO,0).block();
       verify(messageCoreProducerService,times(0)).enqueueMessage(MESSAGE_DTO,0);
       verify(sendNotificationService,times(0)).sendNotify(MESSAGE,TPP_DTO,0);

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
        // IDEMPOTENZA "insert-and-catch": insert() su un _id già esistente (redelivery Kafka)
        // genera DuplicateKeyException. Va trattato come no-op: niente notifica, niente re-enqueue.
        Mockito.when(citizenService.getCitizenConsentsEnabled(any()))
                .thenReturn(Mono.just(TPP_ID_STRING_LIST));

        Mockito.when(tppService.filterEnabledList(any()))
                .thenReturn(Mono.just(TPP_DTO_LIST));

        Mockito.when(messageMapperDTOToObject.map(any(), any(), any(), any()))
                .thenReturn(MESSAGE);

        Mockito.when(messageRepository.insert(Mockito.<Message>any()))
                .thenReturn(Mono.<Message>error(new org.springframework.dao.DuplicateKeyException("duplicate _id")));

        messageService.processMessage(MESSAGE_DTO,0).block();

        verify(sendNotificationService,times(0)).sendNotify(any(),any(),anyLong());
        verify(messageCoreProducerService,times(0)).enqueueMessage(any(),anyLong());
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
