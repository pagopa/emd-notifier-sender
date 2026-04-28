package it.gov.pagopa.common.configuration;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Centralized {@link WebClient.Builder} configuration for all HTTP connectors.
 *
 * <p>Addresses "stale connection" issues caused by Azure load balancers silently
 * closing idle TCP connections. The pool is tuned with explicit {@code maxIdleTime}
 * and {@code maxLifeTime} so connections are recycled before the infrastructure
 * layer discards them.
 *
 * <p>Also adds mandatory connect / read / write timeouts that Netty does not
 * set by default, preventing thread hangs in reactive pipelines.
 */
@Configuration
public class WebClientConfig {

    /** Maximum time a connection may sit idle in the pool before being evicted. */
    private static final Duration MAX_IDLE_TIME = Duration.ofSeconds(20);

    /** Maximum total lifetime of a pooled connection regardless of activity. */
    private static final Duration MAX_LIFE_TIME = Duration.ofSeconds(60);

    /**
     * Maximum number of live connections <strong>per remote host</strong>.
     *
     * <p>Reactor Netty maintains one independent connection bucket per (host, port)
     * pair inside the same {@link ConnectionProvider}. This means that each
     * connector gets its own isolated bucket of up to {@value} connections —
     * there is no contention between them.
     */
    private static final int MAX_CONNECTIONS = 50;

    /**
     * Maximum number of requests that may queue waiting for a connection when the
     * pool for that remote host is full. Excess requests are immediately rejected
     * with a {@code PoolAcquirePendingLimitException} (fail-fast).
     */
    private static final int PENDING_ACQUIRE_MAX_COUNT = MAX_CONNECTIONS * 2;

    /** TCP connect timeout in milliseconds. */
    private static final int CONNECT_TIMEOUT_MS = 5_000;

    /** Read / write timeout in seconds applied via Netty pipeline handlers. */
    private static final int IO_TIMEOUT_SECONDS = 10;

    /** End-to-end response timeout (hard cap for the whole request). */
    private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(15);

    /** Fail-fast cap for waiting on a free connection when the pool is saturated. */
    private static final Duration PENDING_ACQUIRE_TIMEOUT = Duration.ofSeconds(5);

    /** Period of the background eviction task that removes stale connections proactively. */
    private static final Duration EVICT_IN_BACKGROUND = Duration.ofSeconds(30);

    /**
     * Exposes a pre-configured {@link WebClient.Builder} bean.
     *
     * <p><strong>Scope prototype:</strong> each injection point receives its own
     * independent builder instance, preventing baseUrl/header mutations in one
     * connector from leaking into another. The underlying {@link HttpClient} and
     * {@link ConnectionProvider} are singletons (shared pool).
     *
     * @param httpClient the shared singleton {@link HttpClient}
     * @return a fresh, independent {@link WebClient.Builder} per injection
     */
    @Bean
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public WebClient.Builder webClientBuilder(HttpClient httpClient) {
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));
    }

    /**
     * Singleton {@link HttpClient} shared by all {@link WebClient} instances.
     *
     * @param connectionProvider the shared pool provider
     * @return configured {@link HttpClient}
     */
    @Bean
    public HttpClient httpClient(ConnectionProvider connectionProvider) {
        return HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MS)
                .responseTimeout(RESPONSE_TIMEOUT)
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(IO_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(IO_TIMEOUT_SECONDS, TimeUnit.SECONDS)));
    }

    /**
     * Singleton {@link ConnectionProvider} — the actual Netty connection pool.
     *
     * <p>Named {@code emd-notifier-sender-http-pool} so that Micrometer metrics
     * are distinguishable from the pools of other microservices.
     *
     * @return the shared {@link ConnectionProvider}
     */
    @Bean
    public ConnectionProvider connectionProvider() {
        return ConnectionProvider
                .builder("emd-notifier-sender-http-pool")
                .maxConnections(MAX_CONNECTIONS)
                .pendingAcquireMaxCount(PENDING_ACQUIRE_MAX_COUNT)
                .pendingAcquireTimeout(PENDING_ACQUIRE_TIMEOUT)
                .maxIdleTime(MAX_IDLE_TIME)
                .maxLifeTime(MAX_LIFE_TIME)
                .evictInBackground(EVICT_IN_BACKGROUND)
                .metrics(true)
                .build();
    }
}

