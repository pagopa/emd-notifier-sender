package it.gov.pagopa.notifier.dto;


import com.fasterxml.jackson.annotation.JsonAlias;
import it.gov.pagopa.common.utils.CommonUtilities;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@AllArgsConstructor
@Data
@NoArgsConstructor
@Builder
public class MessageDTO {
    private String messageId;
    private String recipientId;
    private String triggerDateTime;
    private String senderDescription;
    private String messageUrl;
    private String originId;
    @JsonAlias("message")
    private String content;
    private String idPsp;
    private Boolean associatedPayment;


    @Override
    public String toString() {

        return "MessageDTO{" +
                "messageId='" + messageId + '\'' +
                ", recipientId='" + CommonUtilities.createSHA256(recipientId) + '\'' +
                ", triggerDateTime='" + triggerDateTime + '\'' +
                ", senderDescription='" + senderDescription + '\'' +
                ", messageUrl='" + messageUrl + '\'' +
                ", originId='" + originId + '\'' +
                ", idPsp='" + idPsp + '\'' +
                ", associatedPayment='" + associatedPayment + '\'' +
                '}';
    }
}
