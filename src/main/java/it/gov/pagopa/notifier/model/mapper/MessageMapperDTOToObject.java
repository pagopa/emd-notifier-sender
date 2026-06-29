package it.gov.pagopa.notifier.model.mapper;

import it.gov.pagopa.notifier.dto.MessageDTO;
import it.gov.pagopa.notifier.enums.MessageState;
import it.gov.pagopa.notifier.model.Message;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class MessageMapperDTOToObject {

    /**
     * Builds the deterministic Mongo {@code _id} from the natural key (messageId + entityId).
     * Using a deterministic id turns {@code repository.save()} into an idempotent upsert:
     * reprocessing the same Kafka message (at-least-once delivery) overwrites the same
     * document instead of creating a duplicate.
     */
    public static String buildId(String messageId, String entityId) {
        return messageId + ":" + entityId;
    }

    public Message map(MessageDTO messageDTO, String idPsp, String entityId, MessageState messageState){
        return Message.builder()
                .id(buildId(messageDTO.getMessageId(), entityId))
                .associatedPayment(messageDTO.getAssociatedPayment())
                .content(messageDTO.getContent())
                .title(messageDTO.getTitle())
                .content(messageDTO.getContent())
                .entityId(entityId)
                .idPsp(idPsp)
                .messageId(messageDTO.getMessageId())
                .messageUrl(messageDTO.getMessageUrl())
                .originId(messageDTO.getOriginId())
                .recipientId(messageDTO.getRecipientId())
                .senderDescription(messageDTO.getSenderDescription())
                .channel(messageDTO.getChannel())
                .triggerDateTime(messageDTO.getTriggerDateTime())
                .messageRegistrationDate(String.valueOf(LocalDateTime.now()))
                .messageState(messageState)
                .analogSchedulingDate(messageDTO.getAnalogSchedulingDate())
                .workflowType(messageDTO.getWorkflowType())
                .build();
    }
}
