package it.gov.pagopa.notifier.connector.citizen;

import it.gov.pagopa.common.configuration.WebClientRetrySpecs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@Slf4j
public class CitizenConnectorImpl implements CitizenConnector {

    private final WebClient webClient;

    /**
     * @param webClientBuilder pre-configured builder from {@code WebClientConfig}
     * @param baseUrl          base URL of the emd-citizen service
     */
    public CitizenConnectorImpl(WebClient.Builder webClientBuilder,
                                @Value("${rest-client.citizen.baseUrl}") String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Idempotent GET → permissive retry on any transient network error.
     */
    @Override
    public Mono<List<String>> getCitizenConsentsEnabled(String fiscalCode) {
        return webClient.get()
                .uri("/emd/citizen/list/{fiscalCode}/enabled/tpp", fiscalCode)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<String>>() {})
                .retryWhen(WebClientRetrySpecs.transientNetwork())
                .doOnError(ex -> log.error(
                        "[CITIZEN-CONNECTOR] GET /emd/citizen/list/{{fiscalCode}}/enabled/tpp failed: {}",
                        ex.getMessage()));
    }
}
