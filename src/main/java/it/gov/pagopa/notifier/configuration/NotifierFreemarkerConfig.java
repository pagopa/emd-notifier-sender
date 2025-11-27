package it.gov.pagopa.notifier.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import freemarker.template.TemplateExceptionHandler;
import org.springframework.context.annotation.Primary;

@Configuration
public class NotifierFreemarkerConfig {

    @Bean("jsonTemplateEngine")
    @Primary
    public freemarker.template.Configuration freemarkerJsonConfig() {
        freemarker.template.Configuration cfg = new freemarker.template.Configuration(freemarker.template.Configuration.VERSION_2_3_32);
        cfg.setDefaultEncoding("UTF-8");

        cfg.setOutputFormat(freemarker.core.JSONOutputFormat.INSTANCE);

        cfg.setLocalizedLookup(false);
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        return cfg;
    }
}