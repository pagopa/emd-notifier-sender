package it.gov.pagopa.notifier.stub.service;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.notifier.dto.MessageDTO;
import it.gov.pagopa.notifier.model.MessageMapperObjectToDTO;
import it.gov.pagopa.notifier.repository.MessageRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

@Service
public class StubMessageCoreServiceImpl implements StubMessageCoreService {

    private final MessageRepository messageRepository;
    private final MessageMapperObjectToDTO mapperToDTO;

    public StubMessageCoreServiceImpl(MessageRepository messageRepository, MessageMapperObjectToDTO mapperToDTO) {
        this.messageRepository = messageRepository;
        this.mapperToDTO = mapperToDTO;
    }

    @Override
    public Mono<List<MessageDTO>> getMessages(String fiscalCode) {
        String hashedFiscalCode = CommonUtilities.createSHA256(fiscalCode);
        return messageRepository.findByHashedFiscalCode(hashedFiscalCode)
                .collectList()
                .map(messageList -> messageList.stream()
                        .map(message ->  mapperToDTO.map(message,hashedFiscalCode))
                        .toList()
                )
                .defaultIfEmpty(Collections.emptyList());
    }


}
