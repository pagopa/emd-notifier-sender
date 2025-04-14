package it.gov.pagopa.notifier.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeleteRequestDTO{

    private FilterDTO filterDTO = new FilterDTO();

    private Integer batchSize;
    private Integer intervalMs;

}