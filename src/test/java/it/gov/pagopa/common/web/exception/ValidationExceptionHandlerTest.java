package it.gov.pagopa.common.web.exception;


import it.gov.pagopa.common.web.dto.ErrorDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;

@WebFluxTest(value = {ValidationExceptionHandlerTest.TestController.class}, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@ContextConfiguration(classes = {
        ValidationExceptionHandlerTest.TestController.class,
        ValidationExceptionHandler.class})
class ValidationExceptionHandlerTest {

    @Autowired
    private WebTestClient webTestClient;


    @RestController
    static class TestController {

        @PutMapping("/test")
        String testEndpoint(@RequestBody @Valid ValidationDTO body, @RequestHeader("data") String data) {
            return "OK";
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class ValidationDTO {
        @NotBlank(message = "The field is mandatory!")
        private String data;
    }

    @Test
    void testHandleValueNotValidException() {
        String invalidJson = "{}";
        webTestClient.put()
                .uri("/test")
                .contentType(MediaType.APPLICATION_JSON)
                .header("data", "someValue")
                .bodyValue(invalidJson)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorDTO.class)
                .consumeWith(response -> {
                    ErrorDTO errorDTO = response.getResponseBody();
                    assertThat(errorDTO).isNotNull();
                    assertThat(errorDTO.getCode()).isEqualTo("INVALID_REQUEST");
                    assertThat(errorDTO.getMessage()).isEqualTo("[data]: The field is mandatory!");
                });
    }
    @Test
    void testHandleHeaderNotValidException() {
        webTestClient.put()
                .uri("/test")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ValidationDTO("data"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorDTO.class)
                .consumeWith(response -> {
                    ErrorDTO errorDTO = response.getResponseBody();
                    assertThat(errorDTO).isNotNull();
                    assertThat(errorDTO.getCode()).isEqualTo("INVALID_REQUEST");
                    assertThat(errorDTO.getMessage()).isEqualTo("Invalid request");

                });
    }

    @Test
    void testHandleNoResourceFoundException() {
        webTestClient.put()
                .uri("/test/missing")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ValidationDTO("someData"))
                .exchange()
                .expectStatus().isNotFound()  // Expect 404 Not Found
                .expectBody(ErrorDTO.class)
                .consumeWith(response -> {
                    ErrorDTO errorDTO = response.getResponseBody();
                    assertThat(errorDTO).isNotNull();
                    assertThat(errorDTO.getCode()).isEqualTo("INVALID_REQUEST");  // Check the code from ErrorDTO
                    assertThat(errorDTO.getMessage()).isEqualTo("Invalid request");  // Check the message from ErrorDTO
                });
    }
}
