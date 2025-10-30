package it.gov.pagopa.notifier.connector.tpp;


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
 * <p>Uses {@link WebClient} to perform HTTP calls to the emd-tpp service.</p>
 */
@Service
@Slf4j
public class TppConnectorImpl implements  TppConnector {
    private final WebClient webClient;


    public TppConnectorImpl(@Value("${rest-client.tpp.baseUrl}") String baseUrl) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    /**
     * {@inheritDoc}
     *
     * @param tppIdList the list of TPP IDs to filter
     * @return {@code Mono<List<TppDTO>>} list of enabled TPPs from emd-tpp service
     */
    @Override
    public Mono<List<TppDTO>> getTppsEnabled(TppIdList tppIdList) {
        return webClient.post()
                .uri("/emd/tpp/list")
                .bodyValue(tppIdList)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<>() {
                });
    }
}
