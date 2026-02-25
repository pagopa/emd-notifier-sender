package it.gov.pagopa.notifier.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TppIdList {
    private List<String> ids;
    private String recipientId;

    public TppIdList(List<String> ids) {
        this.ids = ids;
    }
}
