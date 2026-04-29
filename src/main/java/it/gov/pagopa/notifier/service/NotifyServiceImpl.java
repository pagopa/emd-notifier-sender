package it.gov.pagopa.notifier.service;


import it.gov.pagopa.common.configuration.WebClientRetrySpecs;
import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import it.gov.pagopa.notifier.configuration.DeleteProperties;
import it.gov.pagopa.notifier.dto.*;
import it.gov.pagopa.notifier.enums.MessageState;
import it.gov.pagopa.notifier.model.Message;
import it.gov.pagopa.notifier.repository.MessageRepository;
import it.gov.pagopa.notifier.utils.LogUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;

/**
 * <p>Implementation of {@link NotifyService} for TPP notification delivery and message cleanup.</p>
 */
@Service
@Slf4j
public class NotifyServiceImpl implements NotifyService {

    private final MessageTemplateService messageTemplateService;
    private final WebClient webClient;
    private final NotifyErrorProducerService notifyErrorProducerService;
    private final MessageRepository messageRepository;
    private final DeleteProperties deleteProperties;

    // Reads the same property used by Spring Boot Graceful Shutdown
    // (spring.lifecycle.timeout-per-shutdown-phase), ensuring the .block() timeout
    // in scheduleDeletionTask() is always consistent with the actual shutdown budget.
    // The previous property name (server.shutdown-timeout) was non-standard and silently
    // decoupled from the Helm chart value.
    @Value("${spring.lifecycle.timeout-per-shutdown-phase:20s}")
    private java.time.Duration shutdownTimeout;


    public NotifyServiceImpl(NotifyErrorProducerService notifyErrorProducerService,
                             MessageRepository messageRepository,
                             DeleteProperties deleteProperties,
                             MessageTemplateService messageTemplateService,
                             WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
        this.notifyErrorProducerService = notifyErrorProducerService;
        this.messageRepository = messageRepository;
        this.deleteProperties = deleteProperties;
        this.messageTemplateService = messageTemplateService;
    }


  /**
   * <p>Scheduled task that triggers automatic cleanup of old messages.</p>
   *
   * <p>Executes according to the cron expression defined in {@code delete.batchExecutionCron}.
   * Delegates to {@link #cleanupOldMessages()} e blocca il thread dello scheduler
   * per un massimo di {@code shutdownTimeoutSeconds} secondi.</p>
   *
   * <p><b>FIX Graceful Shutdown:</b> invece di {@code .subscribe()} (fire-and-forget),
   * viene usato {@code .block(timeout)} sul thread dello scheduler (thread bloccante,
   * non su un thread Reactor non-blocking). Questo garantisce che Spring possa attendere
   * il completamento del task durante la fase di shutdown prima di distruggere il contesto.</p>
   */
    @Scheduled(cron = "${delete.batchExecutionCron}")
    public void scheduleDeletionTask() {
        log.info("Start batch");
        // FIX: .block() sul thread TaskScheduler (blocking-safe) invece di .subscribe() fire-and-forget.
        // Spring Boot Graceful Shutdown attenderà il completamento del metodo prima di chiudere il contesto.
        try {
            DeleteResponseDTO response = cleanupOldMessages()
                    .block(shutdownTimeout);
            if (response != null) {
                log.info("Fine batch di eliminazione - Cancellati: {}, Rimasti: {}, Tempo: {}ms",
                        response.getDeletedCount(), response.getRemainingCount(), response.getElapsedTime());
            }
        } catch (Exception e) {
            log.error("Errore nel batch di eliminazione: {}", e.getMessage(), e);
        }
    }

  /**
   * <p>Cleans up old messages based on the retention period defined in configuration.</p>
   *
   * <p>Calculates the cutoff date as {@code now - retentionPeriodDays} and delegates
   * to {@link #deleteMessages(DeleteRequestDTO)} with date filter.</p>
   *
   * @return {@code Mono<DeleteResponseDTO>} with deletion statistics
   */
    public Mono<DeleteResponseDTO> cleanupOldMessages(){
        String retentionDate = LocalDate.now().minusDays(deleteProperties.getRetentionPeriodDays()).toString();
        DeleteRequestDTO deleteRequestDTO = new DeleteRequestDTO();
        FilterDTO filterDTO = new FilterDTO();
        filterDTO.setEndDate(retentionDate);
        deleteRequestDTO.setFilterDTO(filterDTO);
        return deleteMessages(deleteRequestDTO);
    }


  /**
   * {@inheritDoc}
   *
   * <p>Flow:</p>
   * <ol>
   *   <li>Extracts or defaults batch size and interval from request/properties</li>
   *   <li>Determines date range filter (defaults to all dates if not specified)</li>
   *   <li>Queries messages via {@link MessageRepository#findByMessageRegistrationDateBetween(String, String)}</li>
   *   <li>Processes deletions in batches with configured interval delays</li>
   *   <li>Accumulates deleted count and calculates remaining count</li>
   *   <li>Returns statistics with elapsed time</li>
   * </ol>
   *
   * <p>Throws {@code ClientExceptionWithBody} with {@code MESSAGES_NOT_FOUND} if no messages match criteria.</p>
   */
  public Mono<DeleteResponseDTO> deleteMessages(DeleteRequestDTO deleteRequestDTO) {
      int batchSize = (deleteRequestDTO.getBatchSize() != null) ? deleteRequestDTO.getBatchSize() : deleteProperties.getBatchSize();
      int intervalMS = (deleteRequestDTO.getIntervalMs() != null) ? deleteRequestDTO.getIntervalMs() : deleteProperties.getIntervalMs();

      String currentDate = LocalDate.now().plusDays(1).toString();
      String initialDate = LocalDate.MIN.toString();

      String startDate = deleteRequestDTO.getFilterDTO().getStartDate() == null ? initialDate : deleteRequestDTO.getFilterDTO().getStartDate();
      String endDate = deleteRequestDTO.getFilterDTO().getEndDate() == null ? currentDate : deleteRequestDTO.getFilterDTO().getEndDate();

      // FIX double-subscription: the original code subscribed messagesToDelete twice (hasElements() + iteration),
      // causing two separate MongoDB queries. The second query could return different (or empty) results,
      // leading to silent no-ops or incorrect counts. switchIfEmpty on the buffered Flux ensures
      // a single query is executed and the empty case is handled correctly in the same chain.
      Flux<Message> messagesToDelete;
      if (deleteRequestDTO.getFilterDTO().getStartDate() != null || deleteRequestDTO.getFilterDTO().getEndDate() != null) {
          messagesToDelete = messageRepository.findByMessageRegistrationDateBetween(startDate, endDate);
      } else {
          messagesToDelete = messageRepository.findAll();
      }

      long startTime = System.nanoTime();

      return messagesToDelete
          .buffer(batchSize)
          .switchIfEmpty(Flux.error(new ClientExceptionWithBody(
                  HttpStatus.NOT_FOUND, "MESSAGES_NOT_FOUND", "MESSAGES_NOT_FOUND")))
          .concatMap(batch -> {
              log.debug("[NOTIFY-SERVICE][DELETE-MESSAGES] Processing batch of {} messages", batch.size());
              return Flux.fromIterable(batch)
                  .flatMap(messageRepository::delete)
                  .then(Mono.just(batch.size()))
                  .delayElement(Duration.ofMillis(intervalMS));
          })
          .reduce(Integer::sum)
          .flatMap(deletedCount ->
              messageRepository.count()
                  .map(remainingCount -> {
                      long endTime = System.nanoTime();
                      long elapsedTime = (endTime - startTime) / 1_000_000;
                      log.info("[NOTIFY-SERVICE][DELETE-MESSAGES] Deletion complete. Deleted: {}, Remaining: {}, Time: {}ms", deletedCount, remainingCount, elapsedTime);
                      return new DeleteResponseDTO(deletedCount, remainingCount.intValue(), elapsedTime);
                  })
          );
  }


  /**
   * {@inheritDoc}
   *
   * <p>Flow:</p>
   * <ol>
   *   <li>Obtains OAuth2 token via {@link #getToken(TppDTO, String, long)}</li>
   *   <li>Sends notification via {@link #toUrl(Message, TppDTO, TokenDTO, long)}</li>
   *   <li>On success, updates message state to {@code SENT}</li>
   *   <li>On failure, re-enqueues via {@link NotifyErrorProducerService#enqueueNotify(Message, TppDTO, long)} with {@code retry + 1}</li>
   * </ol>
   */
    public Mono<Void> sendNotify(Message message, TppDTO tppDTO, long retry) {
        log.info("[NOTIFY-SERVICE][SEND-NOTIFY] Starting notification process for message ID: {} to TPP: {} at retry: {}",
                message.getMessageId(), tppDTO.getTppId(), retry);

        return getToken(tppDTO, message.getMessageId(), retry)
                .flatMap(token -> toUrl(message, tppDTO, token, retry))
                .onErrorResume(e -> notifyErrorProducerService.enqueueNotify(message,tppDTO,retry + 1))
                .then();
    }

    /**
     * <p>Obtains an OAuth2 authentication token from the TPP's authentication endpoint.</p>
     *
     * <p>Flow:</p>
     * <ol>
     *   <li>Constructs authentication URL by replacing path placeholders from {@code pathAdditionalProperties}</li>
     *   <li>Builds form data from {@code bodyAdditionalProperties}</li>
     *   <li>Posts to authentication endpoint with configured content type</li>
     *   <li>Returns {@link TokenDTO} containing access token</li>
     * </ol>
     *
     *
     * @param tppDTO TPP configuration with authentication URL and token section
     * @param messageId message identifier (for logging)
     * @param retry current retry attempt (for logging)
     * @return {@code Mono<TokenDTO>} containing access token
     */
    private Mono<TokenDTO> getToken(TppDTO tppDTO, String messageId, long retry) {

        log.info("[NOTIFY-SERVICE][GET-TOKEN] Requesting token for message ID: {} to TPP: {} at retry: {}", messageId, tppDTO.getTppId(), retry);

        String urlWithTenant = tppDTO.getAuthenticationUrl();

        if(tppDTO.getTokenSection().getPathAdditionalProperties()!=null) {
            for(Map.Entry<String, String> entry : tppDTO.getTokenSection().getPathAdditionalProperties().entrySet()){
                urlWithTenant = urlWithTenant.replace(entry.getKey(),entry.getValue());
            }
        }

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        if(tppDTO.getTokenSection().getBodyAdditionalProperties()!=null) {
            for(Map.Entry<String, String> entry : tppDTO.getTokenSection().getBodyAdditionalProperties().entrySet()){
                formData.add(entry.getKey(),entry.getValue());
                urlWithTenant = urlWithTenant.replace(entry.getKey(),entry.getValue());
            }
        }

        return webClient.post()
            .uri(urlWithTenant)
            .contentType(MediaType.valueOf(tppDTO.getTokenSection().getContentType()))
            .bodyValue(formData)
            .retrieve()
            .bodyToMono(TokenDTO.class)
            .retryWhen(WebClientRetrySpecs.connectFailureOnly())
            .doOnSuccess(token -> {
                log.info("[NOTIFY-SERVICE][GET-TOKEN] Token successfully obtained for message for message ID: {} to TPP: {} at retry: {}",messageId,tppDTO.getTppId(),retry);
            })
            .doOnError(error -> log.error("[NOTIFY-SERVICE][GET-TOKEN] Error getting token from {}: {}", tppDTO.getEntityId(), error.getMessage()));
    }

    /**
     * <p>Sends the notification message to the TPP's message endpoint using Bearer token authorization.</p>
     *
     * <p>Flow:</p>
     * <ol>
     *   <li>Extracts base message fields via {@link BaseMessage#extractBaseFields(Message)}</li>
     *   <li>Posts JSON payload to TPP's message URL with Bearer token</li>
     *   <li>On success, aggiorna lo stato del messaggio a {@code SENT} e persiste tramite {@code flatMap},
     *       mantenendo la reactive chain integra per il Graceful Shutdown</li>
     *   <li>Logs response and any save errors</li>
     * </ol>
     *
     *
     * @param message the notification message to send
     * @param tppDTO TPP configuration with message URL
     * @param token OAuth2 token for authorization
     * @param retry current retry attempt (for logging)
     * @return {@code Mono<String>} with TPP response body
     */
    private Mono<String> toUrl(Message message, TppDTO tppDTO, TokenDTO token, long retry) {
        log.info("[NOTIFY-SERVICE][TO-URL] Processing MsgId: {} -> Tpp: {} (Try: {})", message.getMessageId(), tppDTO.getEntityId(), retry);

        BaseMessage dataModel = BaseMessage.extractBaseFields(message);

        if (log.isDebugEnabled()) {
            log.debug(
                "[NOTIFY-SERVICE][TO-URL] Trying to render message {} for tpp: {}",
                message.getMessageId(), tppDTO);
        }
        return messageTemplateService.renderTemplate(tppDTO.getMessageTemplate(), dataModel)
            .flatMap(jsonBody -> {
                if (log.isDebugEnabled()) {
                    log.debug("[NOTIFY-SERVICE][TO-URL] Payload MsgId {}: {}", message.getMessageId(), LogUtils.maskSensitiveData(jsonBody));
                }

                return webClient.post()
                    .uri(tppDTO.getMessageUrl())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.getAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(jsonBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .retryWhen(WebClientRetrySpecs.connectFailureOnly());
            })
            // FIX Graceful Shutdown: sostituito doOnSuccess + subscribe() con flatMap.
            // doOnSuccess è un operatore di side-effect: non attende il completamento del publisher interno.
            // Il .subscribe() annidato creava un'operazione fire-and-forget non tracciata da Spring.
            // Con flatMap, il salvataggio su DB è concatenato nella reactive chain principale:
            // Spring attenderà il completamento dell'intera catena prima di terminare il Pod.
            .flatMap(response -> {
                log.info("[NOTIFY-SERVICE][TO-URL] Message {} sent. TPP responded.", message.getMessageId());

                if (log.isDebugEnabled()) {
                    log.debug("[NOTIFY-SERVICE][TO-URL] Response MsgId {}: {}", message.getMessageId(), LogUtils.maskSensitiveData(response));
                }

                message.setMessageState(MessageState.SENT);
                return messageRepository.save(message)
                    .doOnSuccess(saved -> log.info("[NOTIFY-SERVICE][TO-URL] DB Saved SENT. MsgId: {}", saved.getMessageId()))
                    .doOnError(e -> log.error("[NOTIFY-SERVICE][TO-URL] DB Save Failed. MsgId: {}", message.getMessageId(), e))
                    .thenReturn(response);
            })
            .doOnError(error -> {
                log.error("[NOTIFY-SERVICE][TO-URL] Failed MsgId: {} -> Tpp: {}. Reason: {}",
                    message.getMessageId(), tppDTO.getEntityId(), error.getMessage(), error);
            });
    }

}
