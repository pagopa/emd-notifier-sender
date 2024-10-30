package it.gov.pagopa.notifier.connector.citizen;

import it.gov.pagopa.notifier.dto.CitizenConsentDTO;
import reactor.core.publisher.Mono;

import java.util.List;
public interface CitizenConnector {
    Mono<List<CitizenConsentDTO>> getCitizenConsentsEnabled(String fiscalCode);


}
