package it.gov.pagopa.notifier.faker;

import it.gov.pagopa.notifier.dto.TppDTO;

public class TppDTOFaker {
    private TppDTOFaker(){}
    public static TppDTO mockInstance() {
        return TppDTO.builder()
                .tppId("id")
                .build();
    }
}
