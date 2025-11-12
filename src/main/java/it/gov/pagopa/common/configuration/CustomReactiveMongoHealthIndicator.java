package it.gov.pagopa.common.configuration;

import org.bson.Document;
import org.springframework.boot.actuate.health.AbstractReactiveHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

/**
 * Custom health indicator for MongoDB connectivity .
 */
public class CustomReactiveMongoHealthIndicator extends AbstractReactiveHealthIndicator {

    private final ReactiveMongoTemplate reactiveMongoTemplate;

    public CustomReactiveMongoHealthIndicator(ReactiveMongoTemplate reactiveMongoTemplate) {
        super("Mongo health check failed");
        Assert.notNull(reactiveMongoTemplate, "ReactiveMongoTemplate must not be null");
        this.reactiveMongoTemplate = reactiveMongoTemplate;
    }

    /**
     * Performs the MongoDB health check using the {@code isMaster} command.
     *
     * @param builder the health builder
     * @return a Mono emitting the health status with maxWireVersion detail
     */
    @Override
    protected Mono<Health> doHealthCheck(Health.Builder builder)  {
        Mono<Document> buildInfo = this.reactiveMongoTemplate.executeCommand("{ isMaster: 1 }");
        return buildInfo.map(document -> builderUp(builder, document));
    }

    /**
     * Builds a healthy status with MongoDB wire protocol version.
     *
     * @param builder the health builder
     * @param document the MongoDB response document
     * @return the health status marked as UP
     */
    private Health builderUp(Health.Builder builder, Document document) {
        return builder.up().withDetail("maxWireVersion", document.getInteger("maxWireVersion")).build();
    }
}
