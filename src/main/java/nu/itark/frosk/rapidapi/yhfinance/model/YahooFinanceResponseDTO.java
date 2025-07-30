package nu.itark.frosk.rapidapi.yhfinance.model;

import lombok.Data;

@Data
public class YahooFinanceResponseDTO {
    private Meta meta;
    private StatisticsBody body;
}
