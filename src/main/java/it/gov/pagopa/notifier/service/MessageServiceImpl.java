package it.gov.pagopa.notifier.service;

import it.gov.pagopa.common.configuration.MongoRetrySpecs;
import it.gov.pagopa.notifier.connector.citizen.CitizenConnectorImpl;
import it.gov.pagopa.notifier.connector.tpp.TppConnectorImpl;
import it.gov.pagopa.notifier.dto.MessageDTO;
import it.gov.pagopa.notifier.dto.TppDTO;
import it.gov.pagopa.notifier.dto.TppIdList;
import it.gov.pagopa.notifier.enums.MessageState;
import it.gov.pagopa.notifier.model.Message;
import it.gov.pagopa.notifier.model.mapper.MessageMapperDTOToObject;
import it.gov.pagopa.notifier.repository.MessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * <p>Implementation of {@link MessageService} for processing and routing notification messages.</p>
 */
@Slf4j
@Service
public class MessageServiceImpl implements MessageService {


    private final CitizenConnectorImpl citizenConnector;
    private final TppConnectorImpl tppConnector;
    private final MessageCoreProducerServiceImpl messageCoreProducerService;
    private final NotifyServiceImpl sendNotificationService;
    private final MessageRepository messageRepository;
    private final MessageMapperDTOToObject mapperDTOToObject;
    public MessageServiceImpl(CitizenConnectorImpl citizenConnector,
                              TppConnectorImpl tppConnector,
                              MessageCoreProducerServiceImpl messageCoreProducerService, NotifyServiceImpl sendNotificationService,
                              MessageRepository messageRepository,
                              MessageMapperDTOToObject mapperDTOToObject) {
        this.tppConnector = tppConnector;
        this.citizenConnector = citizenConnector;
        this.messageCoreProducerService = messageCoreProducerService;
        this.sendNotificationService = sendNotificationService;
        this.messageRepository = messageRepository;
        this.mapperDTOToObject = mapperDTOToObject;
    }


    /**
     * {@inheritDoc}
     *
     * <p>Flow:</p>
     * <ol>
     *   <li>Queries citizen consents via {@link CitizenConnectorImpl#getCitizenConsentsEnabled(String)}</li>
     *   <li>Delegates to {@link #processTppList(List, MessageDTO, long)} if consents found</li>
     *   <li>On error, delegates to {@link #handleError(Throwable, MessageDTO, long)}</li>
     * </ol>
     *
     * @param messageDTO the message containing recipient and notification details
     * @param retry the current retry attempt count (provided by the consumer)
     * @return {@code Mono<Void>} that completes when all notifications have been sent or processing fails
     */
    @Override
    public Mono<Void> processMessage(MessageDTO messageDTO, long retry) {
        String messageId = messageDTO.getMessageId();
        log.info("[MESSAGE-SERVICE][PROCESS-MESSAGE] Start processing message ID: {} at retry attempt {}", messageId, retry);

        return citizenConnector.getCitizenConsentsEnabled(messageDTO.getRecipientId())
            .doOnNext(ids -> log.debug("[MESSAGE-SERVICE][PROCESS-MESSAGE] Retrieved consent IDs for message ID {}: {}", messageId, ids))
            .flatMap(tppIdList -> processTppList(tppIdList, messageDTO, retry))
            .onErrorResume(e -> handleError(e, messageDTO, retry));
    }

    /**
     * <p>Processes the list of TPP identifiers by fetching configurations and sending notifications.</p>
     *
     * <p>Flow:</p>
     * <ol>
     *   <li>Returns empty if list is empty (no consents)</li>
     *   <li>Fetches TPP configurations via {@link TppConnectorImpl#filterEnabledList(TppIdList)}</li>
     *   <li>Delegates to {@link #sendNotifications(List, MessageDTO, long)}</li>
     * </ol>
     *
     * @param tppIdList list of TPP identifiers with enabled consents
     * @param messageDTO the message DTO
     * @param retry current retry attempt
     * @return {@code Mono<Void>} completing after notifications sent
     */
    private Mono<Void> processTppList(List<String> tppIdList, MessageDTO messageDTO, long retry) {
        String messageId = messageDTO.getMessageId();

        if (tppIdList.isEmpty()) {
            log.info("[MESSAGE-SERVICE][PROCESS-TPP-LIST] No consents found for message ID: {} at retry attempt {}", messageId, retry);
            return Mono.empty();
        }

        log.info("[MESSAGE-SERVICE][PROCESS-TPP-LIST] Consent list found for message ID: {} at retry attempt {}: {}", messageId, retry, tppIdList);

        return tppConnector.filterEnabledList(new TppIdList(tppIdList, messageDTO.getRecipientId()))
            .doOnNext(tppDTOList -> log.debug("[MESSAGE-SERVICE][PROCESS-TPP-LIST] Retrieved TPP DTOs for message ID {}: {}", messageId, tppDTOList))
            .flatMap(tppDTOList -> sendNotifications(tppDTOList, messageDTO, retry));
    }


    /**
     * <p>Sends notifications to each TPP in the list.</p>
     *
     * <p>Flow:</p>
     * <ol>
     *   <li>Returns empty if TPP list is empty</li>
     *   <li>For each TPP, creates the {@code Message} and delegates to {@link #persistAndNotify(MessageDTO, TppDTO, long)}</li>
     * </ol>
     *
     * @param tppDTOList list of TPP configurations
     * @param messageDTO the message DTO
     * @param retry current retry attempt
     * @return {@code Mono<Void>} completing after all sends attempted
     */
    private Mono<Void> sendNotifications(List<TppDTO> tppDTOList, MessageDTO messageDTO, long retry) {
        String messageId = messageDTO.getMessageId();

        if (tppDTOList.isEmpty()) {
            log.info("[MESSAGE-SERVICE][SEND-NOTIFICATIONS] No channels available for message ID: {} at retry attempt {}", messageId, retry);
            return Mono.empty();
        }

        log.info("[MESSAGE-SERVICE][SEND-NOTIFICATIONS] Sending notifications for message ID: {} at retry attempt {}", messageId, retry);

        return Flux.fromIterable(tppDTOList)
            .doOnNext(tpp -> log.debug("[MESSAGE-SERVICE][SEND-NOTIFICATIONS] Processing TPP: {} for message ID: {}", tpp.getTppId(), messageId))
            .flatMap(tppDTO -> persistAndNotify(messageDTO, tppDTO, retry))
            .then();
    }

    /**
     * <p>Persists a brand-new {@code IN_PROCESS} message for the given TPP and triggers the notification.</p>
     *
     * <p><b>Idempotency ("insert-and-catch"):</b> idempotency is enforced by the unique
     * compound index on the natural key {@code (messageId, entityId)}. We use
     * {@link MessageRepository#insert(Object)} (NOT {@code save()}, which would silently
     * upsert/overwrite): if a document with the same natural key already exists, Mongo/CosmosDB
     * raises a {@link DuplicateKeyException} (error 11000). We treat it as a duplicate
     * (Kafka at-least-once redelivery) and turn it into a no-op — no notification, no re-enqueue —
     * without any extra read query. The {@code _id} is auto-generated by Mongo.</p>
     *
     * <p>Throttling (error 16500) is still retried via {@link MongoRetrySpecs#cosmosDbThrottling()};
     * the duplicate-key error (11000) is not throttling, so it is not retried and surfaces here.</p>
     */
    private Mono<Void> persistAndNotify(MessageDTO messageDTO, TppDTO tppDTO, long retry) {
        Message message = mapperDTOToObject.map(messageDTO, tppDTO.getIdPsp(), tppDTO.getEntityId(), MessageState.IN_PROCESS);
        return messageRepository.insert(message)
            .retryWhen(MongoRetrySpecs.cosmosDbThrottling())
            .doOnNext(savedMessage -> log.info("[MESSAGE-SERVICE][SEND-NOTIFICATIONS] Saved IN-PROCESS message ID: {} for entity ID: {}", savedMessage.getMessageId(), savedMessage.getEntityId()))
            .flatMap(savedMessage -> notify(savedMessage, tppDTO, retry))
            // DUPLICATO: documento già presente (redelivery Kafka). No-op: niente notifica, niente re-enqueue.
            .onErrorResume(DuplicateKeyException.class, e -> {
                log.warn("[MESSAGE-SERVICE][SEND-NOTIFICATIONS][DUPLICATE] Message ID: {} for entity ID: {} already exists (duplicate key). Discarding as duplicate.", message.getMessageId(), tppDTO.getEntityId());
                return Mono.empty();
            })
            // Altri errori di persistenza (throttling esaurito, ecc.): non inviamo; il messaggio
            // resterà non processato e verrà ritentato dal flusso di retry a monte.
            .onErrorResume(e -> {
                log.error("[MESSAGE-SERVICE][SEND-NOTIFICATIONS] Error persisting message ID: {} for entity ID: {}. Error: {}", message.getMessageId(), tppDTO.getEntityId(), e.getMessage());
                return Mono.empty();
            });
    }

    /** Sends the notification for a persisted message to the given TPP. */
    private Mono<Void> notify(Message savedMessage, TppDTO tppDTO, long retry) {
        log.info("[MESSAGE-SERVICE][SEND-NOTIFICATIONS] Sending message ID: {} at retry attempt {} to TPP: {}", savedMessage.getMessageId(), retry, tppDTO.getTppId());
        return sendNotificationService.sendNotify(savedMessage, tppDTO, 0)
            .doOnSuccess(v -> log.debug("[MESSAGE-SERVICE][SEND-NOTIFICATIONS] Successfully sent notification to TPP: {} for message ID: {}", tppDTO.getTppId(), savedMessage.getMessageId()))
            .doOnError(e -> log.error("[MESSAGE-SERVICE][SEND-NOTIFICATIONS] Failed sending to TPP: {} for message ID: {}. Error: {}", tppDTO.getTppId(), savedMessage.getMessageId(), e.getMessage()));
    }

    /**
     * <p>Handles processing errors by re-enqueueing the message with incremented retry count.</p>
     *
     * <p>Delegates to {@link MessageCoreProducerServiceImpl#enqueueMessage(MessageDTO, long)}
     * with {@code retry + 1}.</p>
     *
     * @param e the error that occurred
     * @param messageDTO the message DTO
     * @param retry current retry attempt
     * @return {@code Mono<Void>} completing after re-enqueue
     */
    private Mono<Void> handleError(Throwable e, MessageDTO messageDTO, long retry) {
        String messageId = messageDTO.getMessageId();
        log.error("[MESSAGE-SERVICE][HANDLE-ERROR] Error processing message ID: {} at retry attempt {}. Error: {}", messageId, retry, e.getMessage(), e);
        log.info("[MESSAGE-SERVICE][ENQUEUE-WITH-RETRY] Re-enqueuing message ID: {} with increased retry count: {}", messageId, retry + 1);
        return messageCoreProducerService.enqueueMessage(messageDTO, retry + 1);
    }

}
