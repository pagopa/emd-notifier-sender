package it.gov.pagopa.notifier.service;


import it.gov.pagopa.notifier.dto.MessageDTO;

public interface NotifyErrorProducerService {

     void enqueueNotify(MessageDTO messageDTO, String messageUrl, String authenticationUrl, String entityId, long retry);
}
