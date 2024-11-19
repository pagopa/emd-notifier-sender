package it.gov.pagopa.notifier.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotifyErrorQueueMessageDTO {

    private MessageDTO messageDTO;
    private TppDTO tppDTO;
}
