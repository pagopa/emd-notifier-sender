package it.gov.pagopa.notifier.connector.citizen;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static it.gov.pagopa.notifier.utils.TestUtils.FISCAL_CODE;
import static org.assertj.core.api.Assertions.assertThat;


@ExtendWith(MockitoExtension.class)
class CitizenConnectorImplTest {

    private MockWebServer mockWebServer;

    private CitizenConnectorImpl citizenConnector;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        citizenConnector = new CitizenConnectorImpl(WebClient.builder(), mockWebServer.url("/").toString());
        objectMapper = new ObjectMapper();
    }

    @AfterEach
    void tearDown() throws Exception {
        mockWebServer.shutdown();
    }


    @Test
    void testGetCitizenConsentsEnabled() throws JsonProcessingException {

        mockWebServer.enqueue(new MockResponse()
               .setResponseCode(200)
               .setBody(objectMapper.writeValueAsString(List.of("TPP_ID")))
               .addHeader("Content-Type", "application/json"));

        citizenConnector.getCitizenConsentsEnabled(FISCAL_CODE);
        Mono<List<String>> resultMono = citizenConnector.getCitizenConsentsEnabled(FISCAL_CODE);

        List<String> consentList = resultMono.block();
        assertThat(consentList).hasSize(1);
        assertThat(consentList.get(0)).isEqualTo("TPP_ID");
    }

    // ── retry tests ───────────────────────────────────────────────────────────

    private static MockResponse connectionResetResponse() {
        return new MockResponse().setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AT_START);
    }

    /**
     * 2 connection resets + 1 success → retries succeed.
     */
    @Test
    void testGetCitizenConsentsEnabled_retriesOnConnectionResetAndSucceeds() throws Exception {
        mockWebServer.enqueue(connectionResetResponse());
        mockWebServer.enqueue(connectionResetResponse());
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(objectMapper.writeValueAsString(List.of("TPP_ID")))
                .addHeader("Content-Type", "application/json"));

        StepVerifier.create(citizenConnector.getCitizenConsentsEnabled(FISCAL_CODE))
                .assertNext(list -> assertThat(list).hasSize(1))
                .verifyComplete();
    }

    /**
     * 3 consecutive connection resets → retries exhausted, error propagated.
     */
    @Test
    void testGetCitizenConsentsEnabled_exhaustsRetriesAndPropagatesError() {
        mockWebServer.enqueue(connectionResetResponse());
        mockWebServer.enqueue(connectionResetResponse());
        mockWebServer.enqueue(connectionResetResponse());

        StepVerifier.create(citizenConnector.getCitizenConsentsEnabled(FISCAL_CODE))
                .expectErrorMatches(ex ->
                        ex instanceof WebClientRequestException ||
                        (ex.getCause() instanceof WebClientRequestException))
                .verify();
    }
}
