package it.gov.pagopa.notifier.controller;

import it.gov.pagopa.notifier.service.NotifyServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static it.gov.pagopa.notifier.utils.TestUtils.DELETE_REQUEST_DTO;
import static it.gov.pagopa.notifier.utils.TestUtils.DELETE_RESPONSE_DTO;
import static org.mockito.ArgumentMatchers.any;


@WebFluxTest(NotifierSenderController.class)
class NotifierSenderControllerTest {

    @MockBean
    private NotifyServiceImpl notifyService;

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void sendMessage_Ok() {
        Mockito.when(notifyService.deleteMessages(any())).thenReturn(Mono.just(DELETE_RESPONSE_DTO));

        webTestClient.method(HttpMethod.DELETE)
                .uri("/emd/notifier-sender/messages/bulk-delete")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(DELETE_REQUEST_DTO)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(response -> {
                    String resultResponse = response.getResponseBody();
                    Assertions.assertNotNull(resultResponse);
                });
    }


}
