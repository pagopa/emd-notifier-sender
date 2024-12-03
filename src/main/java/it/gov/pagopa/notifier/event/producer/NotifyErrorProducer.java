package it.gov.pagopa.notifier.event.producer;


import it.gov.pagopa.notifier.dto.MessageDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static it.gov.pagopa.notifier.constants.NotifierSenderConstants.MessageHeader.ERROR_MSG_HEADER_TPP_ID;


@Component
@Slf4j
public class NotifyErrorProducer {

  private final String binder;
  private final StreamBridge streamBridge;

  private final ScheduledExecutorService scheduler;

  public NotifyErrorProducer(StreamBridge streamBridge,
                             @Value("${spring.cloud.stream.bindings.notifySender-out-0.binder}")String binder, ScheduledExecutorService scheduler) {
    this.streamBridge = streamBridge;
    this.binder = binder;
    this.scheduler = scheduler;
  }

  public void scheduleMessage(Message<MessageDTO> message) {
    String messageId = message.getPayload().getMessageId();
    String entityId = (String) message.getHeaders().get(ERROR_MSG_HEADER_TPP_ID);
    log.info("[NOTIFY-ERROR-PRODUCER][SCHEDULE-MESSAGE] Scheduling message ID: {} for tppId: {} to notifyErrorQueue with a delay of 5 seconds.", messageId,entityId);

    scheduler.schedule(
            () -> streamBridge.send("notifySender-out-0", binder, message),
            5,
            TimeUnit.SECONDS);
  }

}




