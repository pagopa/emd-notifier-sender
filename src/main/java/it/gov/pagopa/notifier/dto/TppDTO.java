package it.gov.pagopa.notifier.dto;

import it.gov.pagopa.notifier.enums.AuthenticationType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
public class TppDTO {
    private String tppId;
    private String entityId;
    private String businessName;
    private String messageUrl;
    private String authenticationUrl;
    private AuthenticationType authenticationType;
    private Boolean state;
    private LocalDateTime creationDate;
    private LocalDateTime lastUpdateDate;
    private TokenSection tokenSection;
    private String messageTemplate;
}
