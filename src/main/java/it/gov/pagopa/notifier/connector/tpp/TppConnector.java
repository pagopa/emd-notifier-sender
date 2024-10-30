package it.gov.pagopa.notifier.connector.tpp;

import it.gov.pagopa.notifier.dto.TppDTO;
import it.gov.pagopa.notifier.dto.TppIdList;
import reactor.core.publisher.Mono;

import java.util.List;

public interface TppConnector {
    Mono<List<TppDTO>> getTppsEnabled(TppIdList tppIdListIds);
}