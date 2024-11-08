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
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@ExtendWith(MockitoExtension.class)
class CitizenConnectorImplTest {

    private MockWebServer mockWebServer;

    private CitizenConnectorImpl citizenConnector;

    private ObjectMapper objectMapper;

    private final static String FISCAL_CODE = "12345678901";


    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        citizenConnector = new CitizenConnectorImpl(mockWebServer.url("/").toString());

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

}
