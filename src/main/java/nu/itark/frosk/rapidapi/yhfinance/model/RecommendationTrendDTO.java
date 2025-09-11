package nu.itark.frosk.rapidapi.yhfinance.model;

import lombok.Data;

@Data
public class RecommendationTrendDTO {
    private String period;
    private int strongBuy;
    private int buy;
    private int hold;
    private int sell;
    private int strongSell;

    // Convenience method to get total recommendations
    public int getTotalRecommendations() {
        return strongBuy + buy + hold + sell + strongSell;
    }

    // Convenience method to calculate bullish percentage
    public double getBullishPercentage() {
        int total = getTotalRecommendations();
        return total > 0 ? ((double)(strongBuy + buy) / total) * 100 : 0;
    }


}
