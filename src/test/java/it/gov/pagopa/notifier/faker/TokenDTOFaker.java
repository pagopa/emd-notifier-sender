package it.gov.pagopa.notifier.faker;

import it.gov.pagopa.notifier.dto.TokenDTO;

public class TokenDTOFaker {

    private TokenDTOFaker(){}
    public static TokenDTO mockInstance() {
        return TokenDTO.builder()
                .accessToken("accessToken")
                .expiresIn(1)
                .tokenType("tokenType")
                .build();

    }
}
