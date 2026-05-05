package it.gov.pagopa.notifier.configuration;

/**
 * Classe rimossa: il ScheduledExecutorService è stato eliminato.
 *
 * <p>Il delay di 5 secondi tra un retry Kafka e il successivo viveva in un
 * {@code ScheduledExecutorService} non gestito da Spring, causando due problemi critici:</p>
 * <ol>
 *   <li>Il {@code Mono.fromRunnable()} che wrappava {@code scheduler.schedule(...)} completava
 *       immediatamente (task <em>sottomesso</em>), non quando la publish Kafka avveniva realmente
 *       (5 secondi dopo). Di conseguenza {@code BaseKafkaConsumer} committava l'offset Kafka
 *       prima che il messaggio fosse effettivamente inviato → perdita di messaggi a ogni pod restart.</li>
 *   <li>Spring non era a conoscenza dei task in volo su questo executor durante lo shutdown,
 *       quindi li abortiva silenziosamente prima del completamento.</li>
 * </ol>
 *
 * <p>Il delay è stato spostato dentro la reactive chain con {@code Mono.delay(Duration.ofSeconds(5))}
 * in {@code MessageCoreProducerServiceImpl} e {@code NotifyErrorProducerServiceImpl},
 * garantendo che l'offset Kafka venga committato solo dopo la pubblicazione effettiva del messaggio.</p>
 */
public final class SchedulerConfiguration {
    private SchedulerConfiguration() {}
}