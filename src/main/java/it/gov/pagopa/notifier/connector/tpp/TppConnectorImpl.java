package it.gov.pagopa.notifier.connector.tpp;


import it.gov.pagopa.common.configuration.WebClientRetrySpecs;
import it.gov.pagopa.notifier.dto.TppDTO;
import it.gov.pagopa.notifier.dto.TppIdList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * <p>Implementation of {@link TppConnector}.</p>
 *
 * <p>Uses a centrally-configured {@link WebClient} (injected via
 * {@code WebClient.Builder} bean) to perform HTTP calls to the emd-tpp service.</p>
 */
@Service
@Slf4j
public class TppConnectorImpl implements TppConnector {

    private final WebClient webClient;

    /**
     * @param webClientBuilder pre-configured builder from {@code WebClientConfig}
     * @param baseUrl          base URL of the emd-tpp service
     */
    public TppConnectorImpl(WebClient.Builder webClientBuilder,
                            @Value("${rest-client.tpp.baseUrl}") String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Non-idempotent POST → conservative retry only on TCP connect failures.
     */
    @Override
    public Mono<List<TppDTO>> filterEnabledList(TppIdList tppIdList) {
        return webClient.post()
                .uri("/emd/tpp/list")
                .bodyValue(tppIdList)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<TppDTO>>() {})
                .retryWhen(WebClientRetrySpecs.connectFailureOnly())
                .doOnError(ex -> log.error(
                        "[TPP-CONNECTOR] POST /emd/tpp/list failed: {}", ex.getMessage()));
    }
}
