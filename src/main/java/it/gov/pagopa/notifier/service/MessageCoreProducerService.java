package it.gov.pagopa.notifier.service;


import it.gov.pagopa.notifier.dto.MessageDTO;

public interface MessageCoreProducerService {

     void enqueueMessage(MessageDTO messageDTO, long retry);

}
