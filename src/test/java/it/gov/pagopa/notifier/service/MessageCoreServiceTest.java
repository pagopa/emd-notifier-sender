package it.gov.pagopa.notifier.service;

import it.gov.pagopa.notifier.connector.citizen.CitizenConnectorImpl;
import it.gov.pagopa.notifier.connector.tpp.TppConnectorImpl;
import it.gov.pagopa.notifier.custom.CitizenInvocationException;
import it.gov.pagopa.notifier.custom.TppInvocationException;
import it.gov.pagopa.notifier.dto.CitizenConsentDTO;
import it.gov.pagopa.notifier.dto.MessageDTO;
import it.gov.pagopa.notifier.dto.TppDTO;
import it.gov.pagopa.notifier.faker.CitizenConsentDTOFaker;
import it.gov.pagopa.notifier.faker.MessageDTOFaker;
import it.gov.pagopa.notifier.faker.TppDTOFaker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
    SendNotificationServiceImpl sendNotificationService;

    @Autowired
    MessageServiceImpl messageCoreService;

    @Test
    void sendMessage_Ok()  {
        MessageDTO messageDTO = MessageDTOFaker.mockInstance();
        List<CitizenConsentDTO> citizenConsents = List.of(CitizenConsentDTOFaker.mockInstance(true));
        List<TppDTO> tppDTOS = List.of(TppDTOFaker.mockInstance());

        Mockito.when(citizenService.getCitizenConsentsEnabled(any()))
                .thenReturn(Mono.just(citizenConsents));

        Mockito.when(tppService.getTppsEnabled(any()))
                .thenReturn(Mono.just(tppDTOS));


       messageCoreService.processMessage(messageDTO,0).block();
       verify(messageCoreProducerService,times(0)).enqueueMessage(messageDTO,0);

    }

    @Test
    void sendMessage_NoChannelEnabled_Case_NoConsents()  {
        MessageDTO messageDTO = MessageDTOFaker.mockInstance();
        List<CitizenConsentDTO> citizenConsents = List.of();

        Mockito.when(citizenService.getCitizenConsentsEnabled(any()))
                .thenReturn(Mono.just(citizenConsents));

        messageCoreService.processMessage(messageDTO,0).block();
        verify(messageCoreProducerService,times(0)).enqueueMessage(messageDTO,0);

    }

    @Test
    void sendMessage_NoChannelEnabled_Case_NoChannels()  {
        MessageDTO messageDTO = MessageDTOFaker.mockInstance();
        List<CitizenConsentDTO> citizenConsents = List.of(CitizenConsentDTOFaker.mockInstance(true));
        List<TppDTO> tppDTOS = List.of();

        Mockito.when(citizenService.getCitizenConsentsEnabled(any()))
                .thenReturn(Mono.just(citizenConsents));

        Mockito.when(tppService.getTppsEnabled(any()))
                .thenReturn(Mono.just(tppDTOS));

        messageCoreService.processMessage(messageDTO,0).block();
        verify(messageCoreProducerService,times(0)).enqueueMessage(messageDTO,0);

    }

    @Test
    void sendMessage_Ko_CitizenException()  {
        MessageDTO messageDTO = MessageDTOFaker.mockInstance();

        Mockito.when(citizenService.getCitizenConsentsEnabled(any()))
                .thenReturn(Mono.error(new CitizenInvocationException()));

        messageCoreService.processMessage(messageDTO,0).block();
        verify(messageCoreProducerService,times(1)).enqueueMessage(messageDTO,0);

    }

    @Test
    void sendMessage_Ko_TppException()  {
        MessageDTO messageDTO = MessageDTOFaker.mockInstance();
        List<CitizenConsentDTO> citizenConsents = List.of(CitizenConsentDTOFaker.mockInstance(true));

        Mockito.when(citizenService.getCitizenConsentsEnabled(any()))
                .thenReturn(Mono.just(citizenConsents));

        Mockito.when(tppService.getTppsEnabled(any()))
                .thenReturn(Mono.error(new TppInvocationException()));

        messageCoreService.processMessage(messageDTO,0).block();
        verify(messageCoreProducerService,times(1)).enqueueMessage(messageDTO,0);

    }
}
