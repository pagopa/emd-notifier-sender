package it.gov.pagopa.notifier.connector.tpp;

import it.gov.pagopa.notifier.dto.TppDTO;
import reactor.core.publisher.Mono;

public interface TppConnector {
    Mono<TppDTO> getTppEnabled(String tppId);
}