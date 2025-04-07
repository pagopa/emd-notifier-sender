package it.gov.pagopa.notifier.utils.faker;

import it.gov.pagopa.notifier.dto.DeleteRequestDTO;
import it.gov.pagopa.notifier.dto.FilterDTO;

public class DeleteRequestDTOFaker {

    public static DeleteRequestDTO mockInstance() {
        return DeleteRequestDTO.builder()
                .batchSize(300)
                .intervalMs(500)
                .filterDTO(new FilterDTO())
                .build();
    }
}
