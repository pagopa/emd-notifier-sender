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
     * <p>Get list of enabled tpp or tpp with whitelistRecipient field containing the recipientId based on the provided tpp ids.</p>
     * <p>Delegates to the emd-tpp service filtering endpoint.</p>
     *
     * @param tppIdList the list of TPP IDs to filter
     * @return {@code Mono<List<TppDTO>>} list of enabled TPPs matching the provided IDs
     */
    Mono<List<TppDTO>> filterEnabledList(TppIdList tppIdList);
}