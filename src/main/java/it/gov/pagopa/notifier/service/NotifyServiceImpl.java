package it.gov.pagopa.notifier.service;

import it.gov.pagopa.notifier.dto.MessageDTO;
import it.gov.pagopa.notifier.dto.TokenDTO;
import it.gov.pagopa.notifier.model.Message;
import it.gov.pagopa.notifier.dto.mapper.MessageMapperDTOToObject;
import it.gov.pagopa.notifier.repository.MessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class NotifyServiceImpl implements NotifyService {

    private final WebClient webClient;
    private final NotifyErrorProducerService notifyErrorProducerService;

    private final MessageRepository messageRepository;

    private final MessageMapperDTOToObject mapperDTOToObject;
    private final String client;
    private final String clientId;
    private final String grantType;
    private final String tenantId;

    public NotifyServiceImpl(NotifyErrorProducerService notifyErrorProducerService,
                             MessageRepository messageRepository, MessageMapperDTOToObject mapperDTOToObject, @Value("${app.token.client}") String client,
                             @Value("${app.token.clientId}") String clientId,
                             @Value("${app.token.grantType}") String grantType,
                             @Value("${app.token.tenantId}") String tenantId) {
        this.webClient = WebClient.builder().build();
        this.notifyErrorProducerService = notifyErrorProducerService;
        this.messageRepository = messageRepository;
        this.mapperDTOToObject = mapperDTOToObject;
        this.client = client;
        this.clientId = clientId;
        this.grantType = grantType;
        this.tenantId = tenantId;
    }



    public Mono<Void> sendNotify(MessageDTO messageDTO, String messageUrl, String authenticationUrl, String entityId, long retry) {
        log.info("[NOTIFY-SERVICE][SEND-NOTIFY] Starting notification process for message ID: {} to TPP: {} at retry: {}",
                messageDTO.getMessageId(), entityId, retry);

        return getToken(authenticationUrl, messageDTO.getMessageId(), entityId, retry)
                .flatMap(token -> toUrl(messageDTO, messageUrl, token, entityId,retry))
                .onErrorResume(e -> notifyErrorProducerService.enqueueNotify(messageDTO, messageUrl, authenticationUrl, entityId, retry + 1))
                .then();
    }

    private Mono<TokenDTO> getToken(String authenticationUrl,String messageId, String entityId, long retry) {
        String urlWithTenant = authenticationUrl.replace("tenantId", tenantId);

        log.info("[NOTIFY-SERVICE][GET-TOKEN] Requesting token from: {} for message ID: {} to TPP: {} at retry: {}", authenticationUrl,messageId,entityId,retry);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_secret", client);
        formData.add("client_id", clientId);
        formData.add("grant_type", grantType);

        return webClient.post()
                .uri(urlWithTenant)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(formData)
                .retrieve()
                .bodyToMono(TokenDTO.class)
                .doOnSuccess(token -> log.info("[NOTIFY-SERVICE][GET-TOKEN] Token successfully obtained for message for message ID: {} to TPP: {} at retry: {}",messageId,entityId,retry))
                .doOnError(error -> log.error("[NOTIFY-SERVICE][GET-TOKEN] Error getting token from {}: {}", authenticationUrl, error.getMessage()));
    }

    private Mono<String> toUrl(MessageDTO messageDTO, String messageUrl, TokenDTO token, String entityId, long retry) {
        log.info("[NOTIFY-SERVICE][TO-URL] Sending message {} to URL: {} for TPP: {} at try {}", messageDTO.getMessageId(), messageUrl, entityId, retry);

        return webClient.post()
                .uri(messageUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.getAccessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(messageDTO)
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(response -> {
                    log.info("[NOTIFY-SERVICE][TO-URL] Message {} sent successfully at try {}. Response: {}", messageDTO.getMessageId(), retry, response);
                    Message message = mapperDTOToObject.map(messageDTO, entityId);

                    messageRepository.save(message)
                            .doOnSuccess(savedMessage -> log.info("[NOTIFY-SERVICE][TO-URL] Saved message ID: {} for entityId: {}", savedMessage.getMessageId(), entityId))
                            .onErrorResume(error -> {
                                log.error("[NOTIFY-SERVICE][TO-URL] Error saving message ID: {} for entityId: {}", messageDTO.getMessageId(), entityId);
                                return Mono.empty();
                            });
                })
                .doOnError(error -> log.error("[NOTIFY-SERVICE][TO-URL] Error sending message {} at try {} to URL: {}. Error: {}", messageDTO.getMessageId(), retry, messageUrl, error.getMessage()));
    }

}
