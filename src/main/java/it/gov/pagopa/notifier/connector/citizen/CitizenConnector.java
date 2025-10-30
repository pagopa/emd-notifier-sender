package it.gov.pagopa.notifier.connector.citizen;

import reactor.core.publisher.Mono;
import java.util.List;

/**
 * <p>Connector for interacting with the emd-citizen service.</p>
 *
 * <p>Provides remote operations to retrieve citizen consent information.</p>
 */
public interface CitizenConnector {

  /**
   * <p>Retrieves enabled TPP identifiers for a citizen.</p>
   * <p>Delegates to the emd-citizen service enabled TPP list endpoint.</p>
   *
   * @param fiscalCode the citizen's fiscal code
   * @return {@code Mono<List<String>>} list of enabled TPP IDs
   */
  Mono<List<String>> getCitizenConsentsEnabled(String fiscalCode);

}
