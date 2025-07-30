package nu.itark.frosk.rapidapi.yhfinance.model;

import lombok.Data;

import java.util.List;

@Data
public class IncomeStatementHistoryWrapper {
    private List<IncomeStatement> incomeStatementHistory;
    private Integer maxAge;
}
