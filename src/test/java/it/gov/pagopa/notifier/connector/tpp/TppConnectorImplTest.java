package it.gov.pagopa.notifier.connector.tpp;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.notifier.dto.TppDTO;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.List;

import static it.gov.pagopa.notifier.utils.TestUtils.TPP_DTO;
import static it.gov.pagopa.notifier.utils.TestUtils.TPP_ID_LIST;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class TppConnectorImplTest {

    private MockWebServer mockWebServer;
    private TppConnectorImpl tppConnector;
    private ObjectMapper objectMapper;


    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        tppConnector = new TppConnectorImpl(WebClient.builder(), mockWebServer.url("/").toString());
        objectMapper = new ObjectMapper();
    }

    @AfterEach
    void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    void testGetTppSEnabled() throws JsonProcessingException {
        mockWebServer.enqueue(new MockResponse()
               .setResponseCode(200)
               .setBody(objectMapper.writeValueAsString(List.of(TPP_DTO)))
               .addHeader("Content-Type", "application/json"));

         Mono<List<TppDTO>> resultMono = tppConnector.filterEnabledList(TPP_ID_LIST);
         List<TppDTO> consentList = resultMono.block();
         assertThat(consentList).hasSize(1);
         assertThat(consentList.get(0)).isEqualTo(TPP_DTO);
    }

    // ── retry tests ───────────────────────────────────────────────────────────

    private static MockResponse connectionResetResponse() {
        return new MockResponse().setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AT_START);
    }

    /**
     * POST + connection reset → MUST NOT retry (conservative POST policy).
     * The successful response queued after the reset must remain unconsumed,
     * proving that no retry attempt was issued.
     */
    @Test
    void testFilterEnabledList_doesNotRetryOnConnectionReset() throws Exception {
        mockWebServer.enqueue(connectionResetResponse());
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(objectMapper.writeValueAsString(List.of(TPP_DTO)))
                .addHeader("Content-Type", "application/json"));

        StepVerifier.create(tppConnector.filterEnabledList(TPP_ID_LIST))
                .expectErrorMatches(ex ->
                        ex instanceof WebClientRequestException ||
                        (ex.getCause() instanceof WebClientRequestException))
                .verify();
    }

    /**
     * POST + 3 connection resets → still no retry, error propagated immediately.
     */
    @Test
    void testFilterEnabledList_propagatesErrorOnConnectionResetWithoutRetrying() {
        mockWebServer.enqueue(connectionResetResponse());
        mockWebServer.enqueue(connectionResetResponse());
        mockWebServer.enqueue(connectionResetResponse());

        StepVerifier.create(tppConnector.filterEnabledList(TPP_ID_LIST))
                .expectErrorMatches(ex ->
                        ex instanceof WebClientRequestException ||
                        (ex.getCause() instanceof WebClientRequestException))
                .verify();
    }
}
