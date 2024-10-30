package it.gov.pagopa.notifier.connector.tpp;


import it.gov.pagopa.notifier.dto.TppDTO;
import it.gov.pagopa.notifier.dto.TppIdList;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.util.List;

@Service
public class TppConnectorImpl implements  TppConnector {
    private final WebClient webClient;


    public TppConnectorImpl(@Value("${rest-client.tpp.baseUrl}") String baseUrl) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    public Mono<List<TppDTO>> getTppsEnabled(TppIdList tppIdList) {
        return webClient.post()
                .uri("/emd/tpp/list")
                .bodyValue(tppIdList)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<>() {
                });

    }
}
