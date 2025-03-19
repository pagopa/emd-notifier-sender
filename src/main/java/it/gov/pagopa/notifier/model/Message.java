package it.gov.pagopa.notifier.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import it.gov.pagopa.notifier.dto.BaseMessage;
import it.gov.pagopa.notifier.enums.Channel;
import it.gov.pagopa.notifier.enums.MessageState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.mapping.Document;


@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Data
@Document(collection = "message")
public class Message extends BaseMessage {

    @JsonAlias("_id")
    private String id;
    private String entityId;
    private Channel channel;
    private String messageRegistrationDate;
    private MessageState messageState;

}
