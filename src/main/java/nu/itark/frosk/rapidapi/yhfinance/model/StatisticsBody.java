package nu.itark.frosk.rapidapi.yhfinance.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class StatisticsBody {

    private int maxAge;

    // Financial values with full formatting
    @JsonIgnore
    private FinancialValue priceHint;
    @JsonIgnore
    private FinancialValue enterpriseValue;
    @JsonIgnore
    private FinancialValue floatShares;
    @JsonIgnore
    private FinancialValue sharesOutstanding;
    @JsonIgnore
    private FinancialValue impliedSharesOutstanding;
    @JsonIgnore
    private FinancialValue netIncomeToCommon;

    // Simple financial values (raw + fmt only)
    //@JsonProperty("forwardPE")
    //@JsonDeserialize(as = LinkedHashMap.class)
    //private Map<String, Object> forwardPE;
    private Object forwardPE;
    private Object trailingPE;

    private Object profitMargins;
    private Object heldPercentInsiders;
    private Object heldPercentInstitutions;
    private Object beta;
    private Object bookValue;
    private Object priceToBook;
    private Object trailingEps;
    private Object forwardEps;
    private Object enterpriseToRevenue;
    private Object enterpriseToEbitda;
    private Object lastDividendValue;

    // Percentage change values
    @JsonProperty("52WeekChange")
    private SimpleFinancialValue fiftyTwoWeekChange; // Maps to "52WeekChange"
    @JsonProperty("SandP52WeekChange")
    private SimpleFinancialValue sandP52WeekChange; // Maps to "SandP52WeekChange"

    // Date values
    private DateValue lastFiscalYearEnd;
    private DateValue nextFiscalYearEnd;
    private DateValue mostRecentQuarter;
    @JsonIgnore
    private DateValue lastSplitDate;
    @JsonIgnore
    private DateValue lastDividendDate;

    // String values
    private String lastSplitFactor;
    private String category;
    private String fundFamily;
    private String legalType;
    private String latestShareClass;
    private String leadInvestor;

    // Empty array fields (could contain values in other responses)
    @JsonIgnore
    private Object[] sharesShort;
    @JsonIgnore
    private Object[] sharesShortPriorMonth;
    @JsonIgnore
    private Object[] sharesShortPreviousMonthDate;
    @JsonIgnore
    private Object[] dateShortInterest;
    @JsonIgnore
    private Object[] sharesPercentSharesOut;
    @JsonIgnore
    private Object[] shortRatio;
    @JsonIgnore
    private Object[] shortPercentOfFloat;
    @JsonIgnore
    private Object[] morningStarOverallRating;
    @JsonIgnore
    private Object[] morningStarRiskRating;
    @JsonIgnore
    private Object[] annualReportExpenseRatio;
    @JsonIgnore
    private Object[] ytdReturn;
    @JsonIgnore
    private Object[] qtdReturn;
    @JsonIgnore
    private Object[] beta3Year;
    @JsonIgnore
    private Object[] totalAssets;
    @JsonIgnore
    private Object[] yield;
    @JsonIgnore
    private Object[] fundInceptionDate;
    @JsonIgnore
    private Object[] threeYearAverageReturn;
    @JsonIgnore
    private Object[] fiveYearAverageReturn;
    @JsonIgnore
    private Object[] priceToSalesTrailing12Months;
    @JsonIgnore
    private Object[] earningsQuarterlyGrowth;
    @JsonIgnore
    private Object[] revenueQuarterlyGrowth;
    private Object[] pegRatio;
    @JsonIgnore
    private Object[] lastCapGain;
    @JsonIgnore
    private Object[] annualHoldingsTurnover;
    @JsonIgnore
    private Object[] latestFundingDate;
    @JsonIgnore
    private Object[] latestAmountRaised;
    @JsonIgnore
    private Object[] latestImpliedValuation;
    @JsonIgnore
    private Object[] fundingToDate;
    @JsonIgnore
    private Object[] totalFundingRounds;


}
