package it.gov.pagopa.notifier.dto;


import it.gov.pagopa.notifier.enums.AuthenticationType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
public class TppDTO {
    @NotNull
    private String tppId;
    private String entityId;
    private String businessName;
    private String messageUrl;
    private String authenticationUrl;
    private AuthenticationType authenticationType;
    private Contact contact;
    private Boolean state;



}
