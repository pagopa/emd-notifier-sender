package it.gov.pagopa.notifier.connector.tpp;

import it.gov.pagopa.notifier.dto.TppDTO;
import it.gov.pagopa.notifier.dto.TppIdList;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Connector for interacting with the emd-tpp service.
 */
public interface TppConnector {

    /**
     * <p>Retrieves enabled TPPs from a list of TPP identifiers.</p>
     * <p>Delegates to the emd-tpp service filtering endpoint.</p>
     *
     * @param tppIdList the list of TPP IDs to filter
     * @return {@code Mono<List<TppDTO>>} list of enabled TPPs matching the provided IDs
     */
    Mono<List<TppDTO>> getTppsEnabled(TppIdList tppIdList);
}