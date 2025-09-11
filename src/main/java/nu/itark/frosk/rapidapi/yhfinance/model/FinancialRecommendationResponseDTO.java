package nu.itark.frosk.rapidapi.yhfinance.model;

import lombok.Data;

@Data
public class FinancialRecommendationResponseDTO {
    private Meta meta;
    private RecommendationBody body;
}
