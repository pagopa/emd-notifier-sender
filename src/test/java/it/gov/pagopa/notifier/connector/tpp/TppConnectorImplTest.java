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
import reactor.core.publisher.Mono;

import java.io.IOException;

import static it.gov.pagopa.notifier.utils.TestUtils.TPP_DTO;
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

        tppConnector = new TppConnectorImpl(mockWebServer.url("/").toString());

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
               .setBody(objectMapper.writeValueAsString(TPP_DTO))
               .addHeader("Content-Type", "application/json"));

         Mono<TppDTO> resultMono = tppConnector.getTppEnabled(TPP_DTO.getTppId());
         TppDTO tppDTO = resultMono.block();
         assertThat(tppDTO).isEqualTo(TPP_DTO);
    }
}
