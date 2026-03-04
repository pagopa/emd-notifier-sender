package it.gov.pagopa.notifier.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import jakarta.validation.constraints.NotNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TppIdList {
    
    @NotNull
    List<String> ids;

    /**
     * The recipientId used for filtering TPPs based on their whitelistRecipient field.
     */
    @NotNull
    private String recipientId;

}
