package nu.itark.frosk.rapidapi.yhfinance.model;

import lombok.Data;

@Data
public class Body {
    private IncomeStatementHistoryWrapper incomeStatementHistoryQuarterly;
    private IncomeStatementHistoryWrapper incomeStatementHistory;
}
