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
    void map_buildsDeterministicId() {
        Message message = mapper.map(MESSAGE_DTO, TPP_DTO.getIdPsp(), TPP_DTO.getEntityId(), MessageState.IN_PROCESS);

        String expectedId = MESSAGE_DTO.getMessageId() + ":" + TPP_DTO.getEntityId();
        Assertions.assertEquals(expectedId, message.getId());
        Assertions.assertEquals(MessageState.IN_PROCESS, message.getMessageState());
        Assertions.assertEquals(TPP_DTO.getEntityId(), message.getEntityId());
    }

    @Test
    void map_sameNaturalKey_producesSameId() {
        // IDEMPOTENZA: due mappature con stessa chiave naturale -> stesso _id (upsert, non duplicato).
        Message first = mapper.map(MESSAGE_DTO, TPP_DTO.getIdPsp(), TPP_DTO.getEntityId(), MessageState.IN_PROCESS);
        Message second = mapper.map(MESSAGE_DTO, TPP_DTO.getIdPsp(), TPP_DTO.getEntityId(), MessageState.SENT);

        Assertions.assertEquals(first.getId(), second.getId());
    }

    @Test
    void buildId_isStable() {
        Assertions.assertEquals("msg1:entityA", MessageMapperDTOToObject.buildId("msg1", "entityA"));
    }
}

