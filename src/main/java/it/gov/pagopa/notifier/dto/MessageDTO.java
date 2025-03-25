package it.gov.pagopa.notifier.dto;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.notifier.enums.Channel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@Data
@NoArgsConstructor
@SuperBuilder
public class MessageDTO extends BaseMessage {

    private Channel channel;

    @Override
    public String toString() {

        return "MessageDTO{" +
                "messageId='" + getMessageId()+ '\'' +
                ", recipientId='" + CommonUtilities.createSHA256(getRecipientId()) + '\'' +
                ", triggerDateTime='" + getTriggerDateTime() + '\'' +
                ", senderDescription='" + getSenderDescription() + '\'' +
                ", messageUrl='" + getMessageUrl() + '\'' +
                ", originId='" + getOriginId() + '\'' +
                ", content='" + getContent() + '\'' +
                ", idPsp='" + getIdPsp() + '\'' +
                ", channel='" + channel + '\'' +
                '}';
    }
}
