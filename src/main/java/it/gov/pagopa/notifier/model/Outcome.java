package it.gov.pagopa.notifier.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import it.gov.pagopa.notifier.enums.OutcomeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Data
@NoArgsConstructor
@Builder
public class Outcome {
    @JsonAlias("outcome")
    private OutcomeStatus outcomeStatus;
}
