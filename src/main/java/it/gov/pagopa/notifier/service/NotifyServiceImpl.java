package it.gov.pagopa.notifier.service;

import it.gov.pagopa.notifier.dto.MessageDTO;
import it.gov.pagopa.notifier.dto.TokenDTO;
import it.gov.pagopa.notifier.dto.TppDTO;
import it.gov.pagopa.notifier.model.Message;
import it.gov.pagopa.notifier.dto.mapper.MessageMapperDTOToObject;
import it.gov.pagopa.notifier.repository.MessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
@Slf4j
public class NotifyServiceImpl implements NotifyService {

    private final WebClient webClient;
    private final NotifyErrorProducerService notifyErrorProducerService;

    private final MessageRepository messageRepository;

    private final MessageMapperDTOToObject mapperDTOToObject;




    public NotifyServiceImpl(NotifyErrorProducerService notifyErrorProducerService,
                             MessageRepository messageRepository,
                             MessageMapperDTOToObject mapperDTOToObject) {
        this.webClient = WebClient.builder().build();
        this.notifyErrorProducerService = notifyErrorProducerService;
        this.messageRepository = messageRepository;
        this.mapperDTOToObject = mapperDTOToObject;
    }



    public Mono<Void> sendNotify(MessageDTO messageDTO, TppDTO tppDTO, long retry) {
        log.info("[NOTIFY-SERVICE][SEND-NOTIFY] Starting notification process for message ID: {} to TPP: {} at retry: {}",
                messageDTO.getMessageId(), tppDTO, retry);

        return getToken(tppDTO, messageDTO.getMessageId(), retry)
                .flatMap(token -> toUrl(messageDTO, tppDTO, token, retry))
                .onErrorResume(e -> notifyErrorProducerService.enqueueNotify(messageDTO,tppDTO,retry + 1))
                .then();
    }

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
                .doOnSuccess(token -> log.info("[NOTIFY-SERVICE][GET-TOKEN] Token successfully obtained for message for message ID: {} to TPP: {} at retry: {}",messageId,tppDTO.getTppId(),retry))
                .doOnError(error -> log.error("[NOTIFY-SERVICE][GET-TOKEN] Error getting token from {}: {}", tppDTO.getAuthenticationUrl(), error.getMessage()));

    }

    private Mono<String> toUrl(MessageDTO messageDTO, TppDTO tppDTO, TokenDTO token, long retry) {
        log.info("[NOTIFY-SERVICE][TO-URL] Sending message {} to URL: {} for TPP: {} at try {}", messageDTO.getMessageId(), tppDTO.getMessageUrl(), tppDTO.getEntityId(), retry);
        messageDTO.setIdPsp(tppDTO.getIdPsp());
        return webClient.post()
                .uri(tppDTO.getMessageUrl())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.getAccessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(messageDTO)
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(response -> {
                    log.info("[NOTIFY-SERVICE][TO-URL] Message {} sent successfully to TPP {} at try {}. Response: {}", messageDTO.getMessageId(), tppDTO.getEntityId(), retry, response);
                    Message message = mapperDTOToObject.map(messageDTO,tppDTO.getEntityId());

                    messageRepository.save(message)
                            .doOnSuccess(savedMessage -> log.info("[NOTIFY-SERVICE][TO-URL] Saved message ID: {} for entityId: {}", savedMessage.getMessageId(), tppDTO.getEntityId()))
                            .onErrorResume(error -> {
                                log.error("[NOTIFY-SERVICE][TO-URL] Error saving message ID: {} for entityId: {}", messageDTO.getMessageId(), tppDTO.getEntityId());
                                return Mono.empty();
                            })
                            .subscribe();
                })
                .doOnError(error -> log.error("[NOTIFY-SERVICE][TO-URL] Error sending message {} at try {} to URL: {}. Error: {}", messageDTO.getMessageId(), retry, tppDTO.getMessageUrl(), error.getMessage()));
    }

}
