package it.gov.pagopa.common.configuration;

import io.netty.channel.ConnectTimeoutException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.util.retry.Retry;

import java.net.ConnectException;
import java.time.Duration;

/**
 * Centralized {@link Retry} specifications for all {@code WebClient} calls.
 * See javadoc on each method for the trade-offs between resilience and safety.
 */
public final class WebClientRetrySpecs {

    public static final int MAX_RETRY_ATTEMPTS = 2;
    public static final Duration MIN_BACKOFF = Duration.ofMillis(100);
    public static final double JITTER = 0.5;

    private WebClientRetrySpecs() {}

    /**
     * Permissive policy: retries on any {@link WebClientRequestException}.
     * <strong>Use only for idempotent operations</strong> (GET, PUT, DELETE).
     */
    public static Retry transientNetwork() {
        return Retry.backoff(MAX_RETRY_ATTEMPTS, MIN_BACKOFF)
                .jitter(JITTER)
                .filter(ex -> ex instanceof WebClientRequestException);
    }

    /**
     * Conservative policy: retries only when the request demonstrably did not
     * reach the server (TCP handshake failure).
     * <strong>Safe for POST</strong>; does not recover from stale-connection drops.
     */
    public static Retry connectFailureOnly() {
        return Retry.backoff(MAX_RETRY_ATTEMPTS, MIN_BACKOFF)
                .jitter(JITTER)
                .filter(ex -> ex instanceof WebClientRequestException
                        && (ex.getCause() instanceof ConnectException
                            || ex.getCause() instanceof ConnectTimeoutException));
    }
}

