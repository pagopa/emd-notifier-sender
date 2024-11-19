package it.gov.pagopa.notifier.service;

import it.gov.pagopa.notifier.connector.citizen.CitizenConnectorImpl;
import it.gov.pagopa.notifier.connector.tpp.TppConnectorImpl;
import it.gov.pagopa.notifier.custom.CitizenInvocationException;
import it.gov.pagopa.notifier.custom.TppInvocationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;

import static it.gov.pagopa.notifier.utils.TestUtils.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@ContextConfiguration(classes = MessageServiceImpl.class)
class MessageCoreServiceTest {

    @MockBean
    CitizenConnectorImpl citizenService;
    @MockBean
    TppConnectorImpl tppService;
    @MockBean
    MessageCoreProducerServiceImpl messageCoreProducerService;
    @MockBean
    NotifyServiceImpl sendNotificationService;

    @Autowired
    MessageServiceImpl messageCoreService;

    @Test
    void sendMessage_Ok()  {
        Mockito.when(citizenService.getCitizenConsentsEnabled(any()))
                .thenReturn(Mono.just(TPP_ID_STRING_LIST));

        Mockito.when(tppService.getTppsEnabled(any()))
                .thenReturn(Mono.just(TPP_DTO_LIST));

        Mockito.when(sendNotificationService.sendNotify(MESSAGE_DTO,null,RETRY))
                        .thenReturn(Mono.empty());

       messageCoreService.processMessage(MESSAGE_DTO,0).block();
       verify(messageCoreProducerService,times(0)).enqueueMessage(MESSAGE_DTO,0);

    }

    @Test
    void sendMessage_NoChannelEnabled_Case_NoConsents()  {
        Mockito.when(citizenService.getCitizenConsentsEnabled(any()))
                .thenReturn(Mono.just(TPP_ID_STRING_LIST));

        messageCoreService.processMessage(MESSAGE_DTO,0).block();
        verify(messageCoreProducerService,times(0)).enqueueMessage(MESSAGE_DTO,0);

    }

    @Test
    void sendMessage_NoChannelEnabled_Case_NoChannels()  {
        Mockito.when(citizenService.getCitizenConsentsEnabled(any()))
                .thenReturn(Mono.just(TPP_ID_STRING_LIST));

        Mockito.when(tppService.getTppsEnabled(any()))
                .thenReturn(Mono.just(TPP_DTO_LIST));

        messageCoreService.processMessage(MESSAGE_DTO,0).block();
        verify(messageCoreProducerService,times(0)).enqueueMessage(MESSAGE_DTO,0);

    }

    @Test
    void sendMessage_Ko_CitizenException()  {

        Mockito.when(citizenService.getCitizenConsentsEnabled(any()))
                .thenReturn(Mono.error(new CitizenInvocationException()));

        messageCoreService.processMessage(MESSAGE_DTO,0).block();
        verify(messageCoreProducerService,times(1)).enqueueMessage(MESSAGE_DTO,1);

    }

    @Test
    void sendMessage_Ko_TppException()  {

        Mockito.when(citizenService.getCitizenConsentsEnabled(any()))
                .thenReturn(Mono.just(TPP_ID_STRING_LIST));

        Mockito.when(tppService.getTppsEnabled(any()))
                .thenReturn(Mono.error(new TppInvocationException()));

        messageCoreService.processMessage(MESSAGE_DTO,0).block();
        verify(messageCoreProducerService,times(1)).enqueueMessage(MESSAGE_DTO,1);

    }
}
