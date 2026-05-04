package it.gov.pagopa.common.configuration;

import io.netty.channel.ConnectTimeoutException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
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
     * Permissive policy for <strong>idempotent operations</strong> (GET, PUT, DELETE).
     *
     * <p>Retries on transport errors ({@link WebClientRequestException}) and
     * transient HTTP gateway errors (502, 503, 504) common during AKS rolling updates.
     * Does <em>not</em> retry 4xx or non-transient 5xx.
     *
     * @return a fresh {@link Retry} spec — must NOT be reused across pipelines
     */
    public static Retry transientNetwork() {
        return Retry.backoff(MAX_RETRY_ATTEMPTS, MIN_BACKOFF)
                .jitter(JITTER)
                .filter(ex -> {
                    if (ex instanceof WebClientRequestException) {
                        return true;
                    }
                    if (ex instanceof WebClientResponseException responseEx) {
                        int status = responseEx.getStatusCode().value();
                        return status == 502 || status == 503 || status == 504;
                    }
                    return false;
                });
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

