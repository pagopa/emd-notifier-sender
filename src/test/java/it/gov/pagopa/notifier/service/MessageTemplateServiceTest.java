package it.gov.pagopa.notifier.service;

import freemarker.core.JSONOutputFormat;
import freemarker.template.Configuration;
import it.gov.pagopa.notifier.dto.BaseMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class MessageTemplateServiceTest {

    private MessageTemplateService messageTemplateService;
    private Configuration freemarkerConfig;

    @BeforeEach
    void setUp() {
        freemarkerConfig = new Configuration(Configuration.VERSION_2_3_32);
        freemarkerConfig.setDefaultEncoding("UTF-8");
        freemarkerConfig.setOutputFormat(JSONOutputFormat.INSTANCE); 

        messageTemplateService = new MessageTemplateService(freemarkerConfig);
    }

    @Test
    @DisplayName("Should render valid template successfully")
    void renderTemplate_Success() {
        BaseMessage dataModel = BaseMessage.builder()
            .messageId("msg-123")
            .content("Hello World")
            .build();

        String templateContent = "{\"id\": \"${messageId}\", \"text\": \"${content}\"}";

        StepVerifier.create(messageTemplateService.renderTemplate(templateContent, dataModel))
            .expectNextMatches(json ->
                json.contains("\"id\": \"msg-123\"") &&
                    json.contains("\"text\": \"Hello World\"")
            )
            .verifyComplete();
    }

    @Test
    @DisplayName("Should return Error when template string is empty")
    void renderTemplate_EmptyTemplate_ShouldThrowException() {
        BaseMessage dataModel = BaseMessage.builder().build();
        String emptyTemplate = "";

        // ACT & ASSERT
        StepVerifier.create(messageTemplateService.renderTemplate(emptyTemplate, dataModel))
            .expectErrorMatches(throwable ->
                throwable instanceof IllegalArgumentException &&
                    throwable.getMessage().contains("empty Template string")
            )
            .verify();
    }

    @Test
    @DisplayName("Should return Error when template string is null")
    void renderTemplate_NullTemplate_ShouldThrowException() {
        BaseMessage dataModel = BaseMessage.builder().build();
        String nullTemplate = null;

        // ACT & ASSERT
        StepVerifier.create(messageTemplateService.renderTemplate(nullTemplate, dataModel))
            .expectErrorMatches(throwable ->
                throwable instanceof IllegalArgumentException
            )
            .verify();
    }

    @Test
    @DisplayName("Should return Error when FreeMarker fails to parse syntax")
    void renderTemplate_BadSyntax_ShouldThrowException() {
        BaseMessage dataModel = BaseMessage.builder().build();
        String badTemplate = "{\"id\": \"${messageId\"}";

        StepVerifier.create(messageTemplateService.renderTemplate(badTemplate, dataModel))
            .expectError()
            .verify();
    }

    @Test
    @DisplayName("Should return Error when FreeMarker fails processing data")
    void renderTemplate_ProcessingError_ShouldThrowException() {
        BaseMessage dataModel = BaseMessage.builder().build();
        String badLogicTemplate = "${nonExistentField}";

        StepVerifier.create(messageTemplateService.renderTemplate(badLogicTemplate, dataModel))
            .expectError()
            .verify();
    }
}