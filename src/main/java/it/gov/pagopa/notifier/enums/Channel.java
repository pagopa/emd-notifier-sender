package it.gov.pagopa.notifier.enums;

import lombok.Getter;

/**
 * Enum representing the communication channels available for sending messages.
 */
@Getter
public enum Channel {

        SEND("SEND");

    private final String status;

    Channel(String status) {
        this.status = status;
    }

}
