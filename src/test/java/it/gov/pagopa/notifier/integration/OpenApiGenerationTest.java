package it.gov.pagopa.notifier.integration;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

public class OpenApiGenerationTest extends BaseIT {
    
    @Autowired
    private WebTestClient webTestClient;
    
    @Test
    void generateOpenApiFile() throws Exception {

        // 1. Chiama l'endpoint specifico per lo YAML (.yaml)
        //Timeout if use default 5 seconds
        byte[] result = webTestClient.mutate()
                .responseTimeout(Duration.ofSeconds(30))
                .build()
                .get()
                .uri("/v3/api-docs.yaml")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .returnResult()
                .getResponseBody();
                
        if (result != null) {
            Path targetDir = Paths.get("target");
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            String yaml = new String(result, StandardCharsets.UTF_8);
            Files.writeString(targetDir.resolve("openapi.yaml"), yaml);
            System.out.println("✅ OpenAPI YAML generated at: " + targetDir.resolve("openapi.yaml").toAbsolutePath());
        
        } else {
            throw new IllegalStateException("OpenAPI response body was null");
        }
    }
}