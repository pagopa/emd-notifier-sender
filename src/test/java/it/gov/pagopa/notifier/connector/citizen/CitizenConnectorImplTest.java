package it.gov.pagopa.notifier.connector.citizen;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.notifier.dto.CitizenConsentDTO;
import it.gov.pagopa.notifier.faker.CitizenConsentDTOFaker;
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

 @BeforeEach
 void setUp() throws IOException {
  mockWebServer = new MockWebServer();
  mockWebServer.start();

  citizenConnector = new CitizenConnectorImpl(mockWebServer.url("/").toString());
 }

 @AfterEach
 void tearDown() throws Exception {
  mockWebServer.shutdown();
 }


 @Test
 void testGetCitizenConsentsEnabled() throws JsonProcessingException {
   String fiscalCode = "12345678901";
   CitizenConsentDTO citizenConsentDTO = CitizenConsentDTOFaker.mockInstance(true);
   ObjectMapper objectMapper = new ObjectMapper();
   String mockResponseBody = objectMapper.writeValueAsString(List.of(citizenConsentDTO));

   mockWebServer.enqueue(new MockResponse()
           .setResponseCode(200)
           .setBody(mockResponseBody)
           .addHeader("Content-Type", "application/json"));

   citizenConnector.getCitizenConsentsEnabled(fiscalCode);
   Mono<List<CitizenConsentDTO>> resultMono = citizenConnector.getCitizenConsentsEnabled(fiscalCode);

   List<CitizenConsentDTO> consentList = resultMono.block();
   assertThat(consentList).hasSize(1);
   assertThat(consentList.get(0)).isEqualTo(citizenConsentDTO);
 }

}
