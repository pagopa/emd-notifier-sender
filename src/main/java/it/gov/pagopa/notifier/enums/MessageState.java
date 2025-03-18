package it.gov.pagopa.notifier.enums;

import lombok.Getter;

@Getter
public enum MessageState {

    IN_PROCESS("IN_PROCESS"),
    SENT("SENT"),
    ERROR("ERROR");

    private final String status;

    MessageState(String status) {
        this.status = status;
    }

}
