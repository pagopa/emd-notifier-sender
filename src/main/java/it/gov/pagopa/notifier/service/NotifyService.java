package it.gov.pagopa.notifier.service;



import it.gov.pagopa.notifier.dto.DeleteRequestDTO;
import it.gov.pagopa.notifier.dto.DeleteResponseDTO;
import it.gov.pagopa.notifier.dto.TppDTO;
import it.gov.pagopa.notifier.model.Message;
import reactor.core.publisher.Mono;

public interface NotifyService {
    Mono<Void> sendNotify(Message message, TppDTO tppDTO, long retry);

    Mono<DeleteResponseDTO> deleteMessages(DeleteRequestDTO deleteRequestDTO);

}
