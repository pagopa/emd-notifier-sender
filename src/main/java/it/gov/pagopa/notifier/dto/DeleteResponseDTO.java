package it.gov.pagopa.notifier.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeleteResponseDTO {

    private int deletedCount;
    private int remainingCount;
    private long elapsedTime;

}

