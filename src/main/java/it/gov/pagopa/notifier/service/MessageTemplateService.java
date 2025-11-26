package it.gov.pagopa.notifier.service;

import freemarker.template.Template;
import it.gov.pagopa.notifier.dto.BaseMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.io.StringReader;
import java.io.StringWriter;

@Service
@Slf4j
@RequiredArgsConstructor
public class MessageTemplateService {

    private final freemarker.template.Configuration freemarkerConfig;

    /**
     * Renders a FreeMarker template with the provided data model.
     *
     * @param templateContent the FreeMarker template content as a string
     * @param dataModel the data model to populate the template
     * @return {@code Mono<String>} the rendered template as a string
     */
    public Mono<String> renderTemplate(String templateContent, BaseMessage dataModel) {
        return Mono.fromCallable(() -> {
            if (!StringUtils.hasText(templateContent)) {
                throw new IllegalArgumentException("[MESSAGE-TEMPLATE] [RENDER] Received TppDTO with empty Template string!");
            }

            Template t = new Template("runtime_template", new StringReader(templateContent), freemarkerConfig);

            StringWriter out = new StringWriter();
            t.process(dataModel, out);
            String jsonResult = out.toString();

            if (log.isDebugEnabled()) {
                log.debug("[MESSAGE-TEMPLATE] [RENDER] Rendered JSON for messageId: {}:\n{}", dataModel.getMessageId(), jsonResult);
            }

            return jsonResult;
        });
    }
}