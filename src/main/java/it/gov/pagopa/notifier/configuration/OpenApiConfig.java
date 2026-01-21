package it.gov.pagopa.notifier.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.servers.ServerVariable;
import io.swagger.v3.oas.models.servers.ServerVariables;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Value("${spring.application.version}")
    private String appVersion;

    @Bean
    public OpenAPI customOpenAPI() {
        ServerVariable hostVar = new ServerVariable()._default("api.dev.pagopa.it");

        Server hybridServer = new Server()
                .url("https://{host}/emd/notifier-sender") // Protocollo e path fissi, host variabile
                .description("Server Pagopa (Environment variabile)")
                .variables(new ServerVariables().addServerVariable("host", hostVar));

        return new OpenAPI()
                .info(new Info()
                    .title("EMD Notifier Sender Service - Internal S2S API")
                    .version(appVersion)
                    .description(
                        "**⚠️ INTERNAL USE ONLY ⚠️**\n\n" +
                        "Questa specifica documenta l'interfaccia diretta del microservizio Spring Boot.\n" +
                        "Deve essere utilizzata esclusivamente per comunicazioni **Service-to-Service** all'interno del cluster.\n\n" +
                        "👉 Per integrazioni Frontend/Public, fare riferimento alla specifica **Public API (APIM)**."
                    ))
                .servers(List.of(hybridServer));
    }
}