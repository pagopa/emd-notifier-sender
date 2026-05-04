package it.gov.pagopa.notifier.event.producer;


import it.gov.pagopa.notifier.dto.MessageDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class MessageCoreProducer {

  private final String binder;
  private final StreamBridge streamBridge;

  public MessageCoreProducer(StreamBridge streamBridge,
                             @Value("${spring.cloud.stream.bindings.messageSender-out-0.binder}") String binder) {
    this.streamBridge = streamBridge;
    this.binder = binder;
  }

  public void scheduleMessage(Message<MessageDTO> message) {
    String messageId = message.getPayload().getMessageId();
    log.info("[MESSAGE-CORE-PRODUCER][SCHEDULE-MESSAGE] Sending message ID: {} to messageSenderQueue.", messageId);
    streamBridge.send("messageSender-out-0", binder, message);
  }
}



