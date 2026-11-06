package it.gov.pagopa.notifier.repository;

import it.gov.pagopa.notifier.model.Message;
import reactor.core.publisher.Mono;

public interface MessageRepositoryCustom {

	Mono<Message> claimErroredMessage(String messageId, String entityId);

	Mono<Boolean> releaseManualRetryClaim(String messageId, String entityId);
}


