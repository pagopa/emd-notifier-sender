package it.gov.pagopa.notifier.connector.citizen;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * <p>Implementation of {@link CitizenConnector}.</p>
 *
 * <p>Uses {@link WebClient} to perform HTTP calls to the emd-citizen service.</p>
 */
@Service
public class CitizenConnectorImpl implements CitizenConnector {

    private final WebClient webClient;
    public CitizenConnectorImpl( @Value("${rest-client.citizen.baseUrl}") String baseUrl) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();

    }

    /**
     * {@inheritDoc}
     *
     * @param fiscalCode the citizen's fiscal code
     * @return {@code Mono<List<String>>} list of enabled TPP IDs from emd-citizen service
     */
    public Mono<List<String>> getCitizenConsentsEnabled(String fiscalCode) {
        return webClient.get()
                .uri("/emd/citizen/list/{fiscalCode}/enabled/tpp",fiscalCode)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<>() {
                });
    }
}
