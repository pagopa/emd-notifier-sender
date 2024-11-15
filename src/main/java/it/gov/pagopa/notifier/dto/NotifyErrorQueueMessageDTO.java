package it.gov.pagopa.notifier.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NotifyErrorQueueMessageDTO {

    private MessageDTO messageDTO;
    private TppDTO tppDTO;
}
