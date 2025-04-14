package it.gov.pagopa.notifier.utils;

import it.gov.pagopa.notifier.dto.DeleteResponseDTO;

public class DeleteResponseDTOFaker {

    public static DeleteResponseDTO mockInstance() {
        return DeleteResponseDTO.builder()
                .deletedCount(10)
                .elapsedTime(1L)
                .remainingCount(10)
                .build();
    }
}
