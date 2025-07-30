package nu.itark.frosk.rapidapi.yhfinance.model;

import lombok.Data;

@Data
public class Meta {
    private String version;
    private int status;
    private String copywrite;
    private String symbol;
    private String processedTime;
    private String modules;
}
