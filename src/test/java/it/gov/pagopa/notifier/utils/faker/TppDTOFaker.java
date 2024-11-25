package it.gov.pagopa.notifier.utils.faker;

import it.gov.pagopa.notifier.dto.TokenSection;
import it.gov.pagopa.notifier.dto.TppDTO;

import java.util.HashMap;

import static it.gov.pagopa.notifier.utils.TestUtils.AUTHENTICATION_URL;
import static it.gov.pagopa.notifier.utils.TestUtils.MESSAGE_URL;

public class TppDTOFaker {
    private TppDTOFaker(){}
    public static TppDTO mockInstance() {
        return TppDTO.builder()
                .tppId("tppId")
                .entityId("entityId")
                .authenticationUrl(AUTHENTICATION_URL)
                .messageUrl(MESSAGE_URL)
                .tokenSection(mockToken())
                .build();
    }

    private static TokenSection mockToken(){
        HashMap<String,String> map = new HashMap<>();
        map.put("test","test");

        return TokenSection.builder()
                .contentType("application/x-www-form-urlencoded")
                .bodyAdditionalProperties(map)
                .pathAdditionalProperties(map)
                .build();

    }
}
