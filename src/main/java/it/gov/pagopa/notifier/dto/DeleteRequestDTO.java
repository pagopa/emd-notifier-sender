package it.gov.pagopa.notifier.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeleteRequestDTO{

    private Filter filter = new Filter();

    private Integer batchSize;
    private Integer intervalMs;

    public String getStartDate(){
        return filter.getStartDate();
    }

    public String getEndDate(){
        return filter.getEndDate();
    }

}

@Data
class Filter{
    private String startDate;
    private String endDate;
}
