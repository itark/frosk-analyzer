package nu.itark.frosk.rapidapi.yhfinance.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class IncomeStatement {
    private int maxAge;
    private DateValue endDate;
    private FinancialValue totalRevenue;
    private FinancialValue costOfRevenue;
    private FinancialValue grossProfit;
    private FinancialValue totalOperatingExpenses;
    private FinancialValue ebit;
    private FinancialValue incomeTaxExpense;
    private FinancialValue netIncome;
}
