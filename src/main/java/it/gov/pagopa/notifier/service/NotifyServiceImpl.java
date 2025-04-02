package it.gov.pagopa.notifier.service;


import it.gov.pagopa.notifier.configuration.DeleteProperties;
import it.gov.pagopa.notifier.dto.DeleteRequestDTO;
import it.gov.pagopa.notifier.dto.DeleteResponseDTO;
import it.gov.pagopa.notifier.dto.TokenDTO;
import it.gov.pagopa.notifier.dto.TppDTO;
import it.gov.pagopa.notifier.enums.MessageState;
import it.gov.pagopa.notifier.model.Message;
import it.gov.pagopa.notifier.repository.MessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static it.gov.pagopa.notifier.dto.BaseMessage.extractBaseFields;

@Service
@Slf4j
public class NotifyServiceImpl implements NotifyService {

    private final WebClient webClient;
    private final NotifyErrorProducerService notifyErrorProducerService;
    private final MessageRepository messageRepository;
    private final DeleteProperties deleteProperties;
    private final String note;



    public NotifyServiceImpl(NotifyErrorProducerService notifyErrorProducerService,
                             MessageRepository messageRepository,
                             DeleteProperties deleteProperties,
                             @Value("${message-notes}") String note) {
        this.webClient = WebClient.builder().build();
        this.notifyErrorProducerService = notifyErrorProducerService;
        this.messageRepository = messageRepository;
        this.deleteProperties = deleteProperties;
        this.note = note;
    }

    public Mono<DeleteResponseDTO> deleteMessages(DeleteRequestDTO deleteRequestDTO){
        int batchSize = (deleteRequestDTO.getBatchSize() != null) ? deleteRequestDTO.getBatchSize() : deleteProperties.getBatchSize();
        int intervalMS = (deleteRequestDTO.getIntervalMs() != null) ? deleteRequestDTO.getIntervalMs() : deleteProperties.getIntervalMs();

        Flux<Message> messagesToDelete;

        String currentDate = String.valueOf(LocalDateTime.now());
        String initialDate = String.valueOf(LocalDateTime.MIN);
        String endDate = (deleteRequestDTO.getFilterDTO().getEndDate() != null) ? String.valueOf(LocalDateTime.parse(deleteRequestDTO.getFilterDTO().getEndDate(),DateTimeFormatter.ISO_DATE)) : currentDate;
        String startDate = (deleteRequestDTO.getFilterDTO().getStartDate() != null) ? String.valueOf(LocalDateTime.parse(deleteRequestDTO.getFilterDTO().getStartDate(),DateTimeFormatter.ISO_DATE)) : initialDate;
        if(deleteRequestDTO.getFilterDTO().getStartDate() != null || deleteRequestDTO.getFilterDTO().getEndDate() != null){
            messagesToDelete = messageRepository.findByMessageRegistrationDate(startDate, endDate);
        }
        else{
            messagesToDelete = messageRepository.findAll();
        }

        long startTime = System.nanoTime();

        return messagesToDelete
                .buffer(batchSize)
                .concatMap(batch -> Flux.fromIterable(batch)
                        .flatMap(messageRepository::delete)
                        .then(Mono.just(batch.size()))
                        .delayElement(Duration.ofMillis(intervalMS))
                )
                .reduce(Integer::sum)
                .flatMap(deletedCount ->
                    messageRepository.count()
                            .map(remainingCount -> {
                                long endTime = System.nanoTime();
                                long elapsedTime = (endTime - startTime) / 1_000_000;
                                return new DeleteResponseDTO(deletedCount, remainingCount.intValue(), elapsedTime);
                            })
                );
    }



    public Mono<Void> sendNotify(Message message, TppDTO tppDTO, long retry) {
        log.info("[NOTIFY-SERVICE][SEND-NOTIFY] Starting notification process for message ID: {} to TPP: {} at retry: {}",
                message.getMessageId(), tppDTO, retry);

        return getToken(tppDTO, message.getMessageId(), retry)
                .flatMap(token -> toUrl(message, tppDTO, token, retry))
                .onErrorResume(e -> notifyErrorProducerService.enqueueNotify(message,tppDTO,retry + 1))
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
                .doOnError(error -> log.error("[NOTIFY-SERVICE][GET-TOKEN] Error getting token from {}: {}", tppDTO.getEntityId(), error.getMessage()));
    }

    private Mono<String> toUrl(Message message, TppDTO tppDTO, TokenDTO token, long retry) {
        log.info("[NOTIFY-SERVICE][TO-URL] Sending message {} to TPP: {} at try {}", message.getMessageId(), tppDTO.getEntityId(), retry);
        return webClient.post()
                .uri(tppDTO.getMessageUrl())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.getAccessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(extractBaseFields(message, note))
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(response -> {
                    log.info("[NOTIFY-SERVICE][TO-URL] Message {} sent successfully to TPP {} at try {}. Response: {}", message.getMessageId(), tppDTO.getEntityId(), retry, response);
                    message.setMessageState(MessageState.SENT);
                    messageRepository.save(message)
                            .doOnSuccess(savedMessage -> log.info("[NOTIFY-SERVICE][TO-URL] Saved message ID: {} for entityId: {}", savedMessage.getMessageId(), tppDTO.getEntityId()))
                            .onErrorResume(error -> {
                                log.error("[NOTIFY-SERVICE][TO-URL] Error saving message ID: {} for entityId: {}", message.getMessageId(), tppDTO.getEntityId());
                                return Mono.empty();
                            })
                            .subscribe();
                })
                .doOnError(error -> log.error("[NOTIFY-SERVICE][TO-URL] Error sending message {} at try {} to : {}. Error: {}", message.getMessageId(), retry, tppDTO.getEntityId(), error.getMessage()));
    }

}
