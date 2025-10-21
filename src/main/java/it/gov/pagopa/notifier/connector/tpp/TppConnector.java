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
     * Get all the TPPs that are enabled from a list of TPP IDs.
     *
     * @param tppIdList the list of TPP IDs to filter
     * @return a Mono emitting the list of enabled TPPs matching the provided IDs
     */
    Mono<List<TppDTO>> getTppsEnabled(TppIdList tppIdList);
}