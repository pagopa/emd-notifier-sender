package it.gov.pagopa.notifier.connector.citizen;

import reactor.core.publisher.Mono;

import java.util.List;
public interface CitizenConnector {
    Mono<List<String>> getCitizenConsentsEnabled(String fiscalCode);

}
