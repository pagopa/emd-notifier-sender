package it.gov.pagopa.notifier.connector.tpp;


import it.gov.pagopa.notifier.dto.TppDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class TppConnectorImpl implements  TppConnector {
    private final WebClient webClient;


    public TppConnectorImpl(@Value("${rest-client.tpp.baseUrl}") String baseUrl) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    public Mono<TppDTO> getTppEnabled(String tppId) {
        return webClient.get()
                .uri("/emd/tpp/{tppId}/enabled",tppId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<>() {
                });
    }
}
