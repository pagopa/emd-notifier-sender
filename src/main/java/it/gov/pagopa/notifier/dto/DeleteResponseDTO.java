package it.gov.pagopa.notifier.dto;

import it.gov.pagopa.notifier.configuration.DeleteProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeleteResponseDTO {

    private int deletedCount;
    private int remainingCount;
    private long elapsedTime;

}

