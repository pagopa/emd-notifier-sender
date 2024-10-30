package it.gov.pagopa.notifier.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
public class CitizenConsentDTO {
    @JsonAlias("fiscalCode")
    private String hashedFiscalCode;
    @NotNull
    private String tppId;
    private Boolean tppState;
    private LocalDateTime creationDate;
    private LocalDateTime lastUpdateDate;
}
