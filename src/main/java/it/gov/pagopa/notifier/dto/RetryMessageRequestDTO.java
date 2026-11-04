package it.gov.pagopa.notifier.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RetryMessageRequestDTO {

    @NotBlank
    private String messageId;

    @NotBlank
    private String entityId;
}
