package it.gov.pagopa.notifier.event.producer;


import it.gov.pagopa.notifier.dto.MessageDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class NotifyErrorProducer {

  private final String binder;
  private final StreamBridge streamBridge;

  public NotifyErrorProducer(StreamBridge streamBridge,
                             @Value("${spring.cloud.stream.bindings.notifySender-out-0.binder}")String binder) {
    this.streamBridge = streamBridge;
    this.binder = binder;
  }

  public void sendToNotifyErrorQueue(Message<MessageDTO> message) {
    log.info("[EMD-NOTIFIER-SENDER][SEND] Scheduling message {} to notifyErrorQueue",message.getPayload().getMessageId());
    streamBridge.send("notifySender-out-0", binder, message);
  }
}




