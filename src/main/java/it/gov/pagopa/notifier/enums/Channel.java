package it.gov.pagopa.notifier.enums;

import lombok.Getter;

@Getter
public enum Channel {

        SEND("SEND");

    private final String status;

    Channel(String status) {
        this.status = status;
    }

}
