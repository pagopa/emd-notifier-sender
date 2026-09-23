package it.gov.pagopa.notifier.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenDTO {
    @JsonAlias("access_token")
    private String accessToken;
    @JsonAlias("token_type")
    private String tokenType;
    @JsonAlias("expires_in")
    private long expiresIn;
}
