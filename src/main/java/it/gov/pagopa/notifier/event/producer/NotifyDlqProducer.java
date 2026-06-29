package it.gov.pagopa.notifier.event.producer;


import it.gov.pagopa.notifier.dto.MessageDTO;
import it.gov.pagopa.notifier.dto.NotifyErrorQueuePayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * <p>Producer for the Dead Letter Queue (DLQ).</p>
 *
 * <p>Messages that exhausted their retry budget are published here instead of being
 * silently discarded. The DLQ topic has a long retention so that an operator can
 * inspect and manually re-process failed notifications.</p>
 */
@Component
@Slf4j
public class NotifyDlqProducer {

  private final String binder;
  private final StreamBridge streamBridge;

  public NotifyDlqProducer(StreamBridge streamBridge,
                           @Value("${spring.cloud.stream.bindings.notifyDlq-out-0.binder}") String binder) {
    this.streamBridge = streamBridge;
    this.binder = binder;
  }

  /**
   * Publishes the failed payload to the DLQ topic.
   *
   * @param message the message carrying the {@link NotifyErrorQueuePayload} and DLQ headers
   * @return {@code true} if the broker accepted the message
   */
  public boolean sendToDlq(Message<NotifyErrorQueuePayload> message) {
    String messageId = message.getPayload().getMessage().getMessageId();
    String entityId = message.getPayload().getTppDTO().getEntityId();
    log.warn("[NOTIFY-DLQ-PRODUCER][SEND] Routing message ID: {} for entityId: {} to DLQ.", messageId, entityId);
    return streamBridge.send("notifyDlq-out-0", binder, message);
  }

  /**
   * Publishes a failed upstream {@link MessageDTO} (message-core re-enqueue path) to the DLQ.
   *
   * @param message the message carrying the {@link MessageDTO} and DLQ headers
   * @return {@code true} if the broker accepted the message
   */
  public boolean sendMessageDtoToDlq(Message<MessageDTO> message) {
    String messageId = message.getPayload().getMessageId();
    log.warn("[NOTIFY-DLQ-PRODUCER][SEND] Routing upstream message ID: {} to DLQ.", messageId);
    return streamBridge.send("notifyDlq-out-0", binder, message);
  }
}


