package it.gov.pagopa.notifier.connector;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.notifier.connector.tpp.TppConnectorImpl;
import it.gov.pagopa.notifier.dto.TppDTO;
import it.gov.pagopa.notifier.dto.TppIdList;
import it.gov.pagopa.notifier.faker.TppDTOFaker;
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
class TppConnectorImplTest {

 private MockWebServer mockWebServer;

 private TppConnectorImpl tppConnector;

 @BeforeEach
 void setUp() throws IOException {
  mockWebServer = new MockWebServer();
  mockWebServer.start();

  tppConnector = new TppConnectorImpl(mockWebServer.url("/").toString());
 }

 @AfterEach
 void tearDown() throws Exception {
  mockWebServer.shutdown();
 }


 @Test
 void testGetTppsEnabled() throws JsonProcessingException, InterruptedException {
   String tppId = "12345678901";
   TppIdList tppIdList = new TppIdList(List.of(tppId));
   TppDTO tppDTO = TppDTOFaker.mockInstance();
   ObjectMapper objectMapper = new ObjectMapper();
   String mockResponseBody = objectMapper.writeValueAsString(List.of(tppDTO));

   mockWebServer.enqueue(new MockResponse()
           .setResponseCode(200)
           .setBody(mockResponseBody)
           .addHeader("Content-Type", "application/json"));

     Mono<List<TppDTO>> resultMono = tppConnector.getTppsEnabled(tppIdList);
     List<TppDTO> consentList = resultMono.block();
     assertThat(consentList).hasSize(1);
     assertThat(consentList.get(0)).isEqualTo(tppDTO);
  }
}
