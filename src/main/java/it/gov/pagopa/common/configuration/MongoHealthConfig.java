package it.gov.pagopa.common.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

/**
 * Configuration class for MongoDB health check components.
 */
@Configuration
public class MongoHealthConfig {

    /**
     * Creates a custom MongoDB health indicator bean.
     *
     * @param reactiveMongoTemplate the reactive MongoDB template
     * @return a configured {@link CustomReactiveMongoHealthIndicator} instance
     */
    @Bean
    public CustomReactiveMongoHealthIndicator customMongoHealthIndicator(ReactiveMongoTemplate reactiveMongoTemplate) {
        return new CustomReactiveMongoHealthIndicator(reactiveMongoTemplate);
    }
}

