package it.gov.pagopa.notifier.faker;

import it.gov.pagopa.notifier.dto.CitizenConsentDTO;

public class CitizenConsentDTOFaker {

    private CitizenConsentDTOFaker(){}
    public static CitizenConsentDTO mockInstance(Boolean bias) {
        return CitizenConsentDTO.builder()
                .tppId("channelId")
                .tppState(bias)
                .hashedFiscalCode("hashedFiscalCode")
                .build();

    }
}
