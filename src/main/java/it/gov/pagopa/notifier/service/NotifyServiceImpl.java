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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
@Slf4j
public class NotifyServiceImpl implements NotifyService {

    private final WebClient webClient;
    private final NotifyErrorProducerService notifyErrorProducerService;

    private final MessageRepository messageRepository;

    private final MessageMapperDTOToObject mapperDTOToObject;

    private final SecretService secretService;



    public NotifyServiceImpl(NotifyErrorProducerService notifyErrorProducerService,
                             MessageRepository messageRepository,
                             MessageMapperDTOToObject mapperDTOToObject,
                             SecretService secretService) {
        this.secretService = secretService;
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
                .onErrorResume(e -> notifyErrorProducerService.enqueueNotify(messageDTO, tppDTO, retry + 1))
                .then();
    }

    private Mono<TokenDTO> getToken(TppDTO tppDTO, String messageId, long retry) {

        log.info("[NOTIFY-SERVICE][GET-TOKEN] Requesting token for message ID: {} to TPP: {} at retry: {}", messageId, tppDTO.getTppId(), retry);

        return replaceSecrets(tppDTO.getAuthenticationUrl(), tppDTO.getTokenSection().getPathAdditionalProperties())
                .flatMap(authenticationUrl -> {
                    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
                    return replaceSecretsInFormData(tppDTO.getTokenSection().getBodyAdditionalProperties(), formData)
                            .then(Mono.defer(() -> webClient.post()
                                    .uri(authenticationUrl)
                                    .contentType(MediaType.valueOf(tppDTO.getTokenSection().getContentType()))
                                    .bodyValue(formData)
                                    .retrieve()
                                    .bodyToMono(TokenDTO.class)));
                })
                .doOnSuccess(token -> log.info("[NOTIFY-SERVICE][GET-TOKEN] Token successfully obtained for message ID: {} to TPP: {} at retry: {}", messageId, tppDTO.getEntityId(), retry))
                .doOnError(error -> log.error("[NOTIFY-SERVICE][GET-TOKEN] Error getting token: {}", error.getMessage()));

    }
    private Mono<String> replaceSecrets(String url, Map<String, String> pathSecrets) {
        if(pathSecrets != null)
            return Flux.fromIterable(pathSecrets.entrySet())
                    .flatMap(entry -> secretService.getSecret(entry.getValue())
                            .map(secretValue -> Map.entry(entry.getKey(), secretValue))
                    )
                    .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                    .map(secretsMap -> {
                        String updatedUrl = url;
                        for (Map.Entry<String, String> entry : secretsMap.entrySet()) {
                            updatedUrl = updatedUrl.replace(entry.getKey(), entry.getValue());
                        }
                        return updatedUrl;
                    });
        else
            return Mono.just(url);
    }

    private Mono<Void> replaceSecretsInFormData(Map<String, String> bodySecrets, MultiValueMap<String, String> formData) {
        return Flux.fromIterable(bodySecrets.entrySet())
                .flatMap(entry -> secretService.getSecret(entry.getValue())
                        .doOnNext(secretValue -> formData.add(entry.getKey(), secretValue))
                )
                .then();
    }

    private Mono<String> toUrl(MessageDTO messageDTO, TppDTO tppDTO, TokenDTO token, long retry) {
        log.info("[NOTIFY-SERVICE][TO-URL] Sending message {} to URL: {} for TPP: {} at try {}", messageDTO.getMessageId(), tppDTO.getMessageUrl(), tppDTO.getEntityId(), retry);

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
