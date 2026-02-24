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
     * <p>Retrieves enabled or whitelisted TPPs from a list of TPP identifiers for a specific recipient.</p>
     * <p>Delegates to the emd-tpp service filtering endpoint.</p>
     *
     * @param tppIdList the list of TPP IDs to filter
     * @param recipient the recipient's fiscal code to check for whitelist membership
     * @return {@code Mono<List<TppDTO>>} list of enabled or whitelisted TPPs matching the provided IDs
     */
    Mono<List<TppDTO>> getTppsEnabled(TppIdList tppIdList, String recipient);
}