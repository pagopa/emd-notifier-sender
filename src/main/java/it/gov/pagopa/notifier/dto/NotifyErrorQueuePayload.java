package it.gov.pagopa.notifier.dto;


import it.gov.pagopa.notifier.model.Message;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@AllArgsConstructor
@Data
@NoArgsConstructor
@Builder
public class NotifyErrorQueuePayload {

    private TppDTO tppDTO;
    private Message message;
}
