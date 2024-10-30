package it.gov.pagopa.notifier.faker;

import it.gov.pagopa.notifier.enums.OutcomeStatus;
import it.gov.pagopa.notifier.model.Outcome;

public class OutcomeFaker {

    private OutcomeFaker(){}
    public static Outcome mockInstance(Boolean bias) {
        return Outcome.builder()
                .outcomeStatus(bias ? OutcomeStatus.OK : OutcomeStatus.NO_CHANNELS_ENABLED)
                .build();

    }
}
