package it.gov.pagopa.notifier.enums;

import lombok.Getter;


/**
 * Enum representing the notification type.
 */
@Getter
public enum WorkflowType {

        ANALOG("ANALOG"),
        DIGITAL("DIGITAL");

    private final String workflowType;

    WorkflowType(String workflowType) {
        this.workflowType = workflowType;
    }

}
