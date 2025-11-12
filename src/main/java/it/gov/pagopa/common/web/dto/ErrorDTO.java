package it.gov.pagopa.common.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.gov.pagopa.common.web.exception.ServiceExceptionPayload;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Standard error response DTO.
 * <p>
 * Used to return structured error information to clients when
 * exceptions occur during request processing.
 * </p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode
public class ErrorDTO implements ServiceExceptionPayload {

  @NotBlank
  private String code;
  @NotBlank
  private String message;
}
