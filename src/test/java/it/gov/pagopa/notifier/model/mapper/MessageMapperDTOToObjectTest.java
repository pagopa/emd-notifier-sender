package it.gov.pagopa.notifier.model.mapper;

import it.gov.pagopa.notifier.enums.MessageState;
import it.gov.pagopa.notifier.model.Message;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static it.gov.pagopa.notifier.utils.TestUtils.MESSAGE_DTO;
import static it.gov.pagopa.notifier.utils.TestUtils.TPP_DTO;

class MessageMapperDTOToObjectTest {

    private final MessageMapperDTOToObject mapper = new MessageMapperDTOToObject();

    @Test
    void map_buildsMessage() {
        Message message = mapper.map(MESSAGE_DTO, TPP_DTO.getIdPsp(), TPP_DTO.getEntityId(), MessageState.IN_PROCESS);

        // L'_id non viene valorizzato: è generato da Mongo; l'idempotenza è data dall'indice
        // unique compound (messageId, entityId).
        Assertions.assertNull(message.getId());
        Assertions.assertEquals(MESSAGE_DTO.getMessageId(), message.getMessageId());
        Assertions.assertEquals(MessageState.IN_PROCESS, message.getMessageState());
        Assertions.assertEquals(TPP_DTO.getEntityId(), message.getEntityId());
    }
}

