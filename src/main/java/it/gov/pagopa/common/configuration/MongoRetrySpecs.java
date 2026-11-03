package it.gov.pagopa.common.configuration;

import com.mongodb.MongoCommandException;
import lombok.extern.slf4j.Slf4j;
import reactor.util.retry.Retry;

import java.time.Duration;

/**
 * Centralized {@link Retry} specifications for MongoDB / CosmosDB operations.
 *
 * <p>CosmosDB for MongoDB API throttles requests when RU/s are exhausted,
 * returning error code {@code 16500} (TooManyRequests). The MongoDB driver surfaces
 * this as a {@link MongoCommandException}.</p>
 *
 * <p>Applying this retry directly on {@code messageRepository.save()} means:</p>
 * <ul>
 *   <li>Only the failing DB operation is retried, not the entire Kafka message.</li>
 *   <li>The Kafka offset is not committed until the operation succeeds or exhausts retries.</li>
 *   <li>No need for application-level delays in the Kafka producer as a workaround.</li>
 * </ul>
 *
 * <p>This mirrors the pattern already used in {@link WebClientRetrySpecs}.</p>
 */
@Slf4j
public final class MongoRetrySpecs {

    /** CosmosDB for MongoDB API error code for throttling (TooManyRequests / HTTP 429). */
    public static final int COSMOS_DB_TOO_MANY_REQUESTS = 16500;

    public static final int MAX_RETRY_ATTEMPTS = 3;
    public static final Duration MIN_BACKOFF = Duration.ofMillis(500);
    public static final Duration MAX_BACKOFF = Duration.ofSeconds(30);
    public static final double JITTER = 0.5;

    private MongoRetrySpecs() {}

    /**
     * Retry spec for CosmosDB throttling (429 / error code 16500).
     *
     * <p>Uses exponential backoff with jitter: 500ms → ~1s → ~2s → ... up to 30s.
     * Jitter avoids thundering-herd when multiple pods hit 429 simultaneously.</p>
     *
     * <p><strong>Safe for save() operations</strong>: CosmosDB upsert is idempotent
     * when the document already exists (overwrites with same state).</p>
     *
     * @return a fresh {@link Retry} spec — must NOT be reused across pipelines
     */
    public static Retry cosmosDbThrottling() {
        return Retry.backoff(MAX_RETRY_ATTEMPTS, MIN_BACKOFF)
                .maxBackoff(MAX_BACKOFF)
                .jitter(JITTER)
                .filter(MongoRetrySpecs::isThrottled)
                .doBeforeRetry(signal -> log.warn(
                        "[MONGO-RETRY] CosmosDB throttled (429 / error 16500), attempt {}/{}. Backing off.",
                        signal.totalRetries() + 1, MAX_RETRY_ATTEMPTS));
    }

    /**
     * Returns {@code true} if the error is a CosmosDB throttling response.
     * Walks the cause chain to handle Spring Data wrapping.
     */
    public static boolean isThrottled(Throwable e) {
        if (e == null) {
            return false;
        }
        if (e instanceof MongoCommandException mce && mce.getErrorCode() == COSMOS_DB_TOO_MANY_REQUESTS) {
            return true;
        }
        return isThrottled(e.getCause());
    }
}

