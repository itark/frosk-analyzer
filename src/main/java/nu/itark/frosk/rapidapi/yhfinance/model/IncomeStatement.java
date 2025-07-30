package nu.itark.frosk.rapidapi.yhfinance.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.List;

@Data
public class IncomeStatement {
    private int maxAge;
    private DateValue endDate;
    private FinancialValue totalRevenue;
    private FinancialValue costOfRevenue;
    private FinancialValue grossProfit;
    @JsonIgnore
    private List<Object> researchDevelopment;
    private List<Object> sellingGeneralAdministrative;
    private List<Object> nonRecurring;
    private List<Object> otherOperatingExpenses;
    @JsonIgnore
    private FinancialValue totalOperatingExpenses;
    private List<Object> operatingIncome;
    private List<Object> totalOtherIncomeExpenseNet;
    @JsonIgnore
    private FinancialValue ebit;
    private List<Object> interestExpense;
    private List<Object> incomeBeforeTax;
    private FinancialValue incomeTaxExpense;
    private List<Object> minorityInterest;
    private List<Object> netIncomeFromContinuingOps;
    private List<Object> discontinuedOperations;
    private List<Object> extraordinaryItems;
    private List<Object> effectOfAccountingCharges;
    private List<Object> otherItems;
    private FinancialValue netIncome;
    private List<Object> netIncomeApplicableToCommonShares;


}
