package it.gov.pagopa.common.web.exception;

import it.gov.pagopa.common.web.dto.ErrorDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import org.springframework.web.server.MissingRequestValueException;

import java.util.Optional;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ValidationExceptionHandler {

    private final ErrorDTO templateValidationErrorDTO;

    public ValidationExceptionHandler(@Nullable ErrorDTO templateValidationErrorDTO) {
        this.templateValidationErrorDTO = Optional.ofNullable(templateValidationErrorDTO)
                .orElse(new ErrorDTO("INVALID_REQUEST", "Invalid request"));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorDTO handleWebExchangeBindException(
            WebExchangeBindException ex, ServerHttpRequest request) {

        String message = ex.getBindingResult().getAllErrors().stream()
                .map(error -> {
                    String fieldName = ((FieldError) error).getField();
                    String errorMessage = error.getDefaultMessage();
                    return String.format("[%s]: %s", fieldName, errorMessage);
                }).collect(Collectors.joining("; "));

        log.info("A WebExchangeBindException occurred handling request {}: HttpStatus 400 - {}",
                ErrorManager.getRequestDetails(request), message);
        log.debug("Something went wrong while validating http request", ex);

        return new ErrorDTO(templateValidationErrorDTO.getCode(), message);
    }

    @ExceptionHandler(MissingRequestValueException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorDTO handleMissingRequestValueException(MissingRequestValueException e, ServerHttpRequest request) {

        log.info("A MissingRequestValueException occurred handling request {}: HttpStatus 400 - {}",
                ErrorManager.getRequestDetails(request), e.getMessage());
        log.debug("Something went wrong due to a missing request value", e);

        return new ErrorDTO(templateValidationErrorDTO.getCode(), templateValidationErrorDTO.getMessage());
    }
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorDTO handleNoResourceFoundException(NoResourceFoundException e, ServerHttpRequest request) {

        log.info("A NoResourceFoundException occurred handling request {}: HttpStatus 400 - {}",
                ErrorManager.getRequestDetails(request), e.getMessage());
        log.debug("Something went wrong due to a missing request value", e);

        return new ErrorDTO(templateValidationErrorDTO.getCode(), templateValidationErrorDTO.getMessage());
    }

}
