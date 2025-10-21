package it.gov.pagopa.notifier.connector.citizen;

import reactor.core.publisher.Mono;
import java.util.List;

/**
 *  Connector for interacting with the emd-citizen service.
 */
public interface CitizenConnector {

  /**
   * Retrieves the list of TPP IDs for which the citizen has given consent and has enabled.
   *
   * @param fiscalCode the citizen's fiscal code
   * @return a Mono emitting the list of enabled TPP IDs for the citizen
   */
  Mono<List<String>> getCitizenConsentsEnabled(String fiscalCode);

}
