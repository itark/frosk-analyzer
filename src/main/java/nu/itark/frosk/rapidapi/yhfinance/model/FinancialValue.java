package nu.itark.frosk.rapidapi.yhfinance.model;

import lombok.Data;

@Data
public class FinancialValue {
    private long raw;
    private String fmt;
    private String longFmt;
}
