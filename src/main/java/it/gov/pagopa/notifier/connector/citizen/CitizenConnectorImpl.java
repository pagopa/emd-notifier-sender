package it.gov.pagopa.notifier.connector.citizen;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class CitizenConnectorImpl implements CitizenConnector {

    private final WebClient webClient;
    public CitizenConnectorImpl( @Value("${rest-client.citizen.baseUrl}") String baseUrl) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();

    }

    /**
     * {@inheritDoc}
     */
    public Mono<List<String>> getCitizenConsentsEnabled(String fiscalCode) {
        return webClient.get()
                .uri("/emd/citizen/list/{fiscalCode}/enabled/tpp",fiscalCode)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<>() {
                });
    }
}
