package nu.itark.frosk.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity to store recommendation trend snapshots for a security
 */
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "recommendation_trend")
public class RecommendationTrend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "security_id", nullable = false)
    private Security security;

    @Column(name = "period", nullable = false)
    private String period; // "0m", "-1m", "-2m", "-3m"

    @Column(name = "strong_buy", nullable = false)
    private Integer strongBuy;

    @Column(name = "buy", nullable = false)
    private Integer buy;

    @Column(name = "hold", nullable = false)
    private Integer hold;

    @Column(name = "sell", nullable = false)
    private Integer sell;

    @Column(name = "strong_sell", nullable = false)
    private Integer strongSell;

    /**
     * Calculate total number of recommendations
     */
    public int getTotalRecommendations() {
        return strongBuy + buy + hold + sell + strongSell;
    }

    /**
     * Calculate bullish percentage (strongBuy + buy) / total
     */
    public double getBullishPercentage() {
        int total = getTotalRecommendations();
        return total > 0 ? ((double)(strongBuy + buy) / total) * 100 : 0;
    }

    /**
     * Calculate bearish percentage (sell + strongSell) / total
     */
    public double getBearishPercentage() {
        int total = getTotalRecommendations();
        return total > 0 ? ((double)(sell + strongSell) / total) * 100 : 0;
    }

    /**
     * Calculate neutral percentage (hold) / total
     */
    public double getNeutralPercentage() {
        int total = getTotalRecommendations();
        return total > 0 ? ((double)hold / total) * 100 : 0;
    }

    /**
     * Get recommendation strength score (weighted score)
     * StrongBuy=5, Buy=4, Hold=3, Sell=2, StrongSell=1
     */
    public double getRecommendationScore() {
        int total = getTotalRecommendations();
        if (total == 0) return 3.0; // Neutral if no recommendations

        double weightedSum = (strongBuy * 5.0) + (buy * 4.0) + (hold * 3.0) + (sell * 2.0) + (strongSell * 1.0);
        return weightedSum / total;
    }
}
