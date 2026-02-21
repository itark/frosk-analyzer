package nu.itark.frosk.strategies.indicators;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.Num;

public class RecommendationIndicator extends CachedIndicator<Num> {

    private final int strongBuy;
    private final int buy;
    private final int hold;
    private final int sell;
    private final int strongSell;

    public RecommendationIndicator(BarSeries series,
                                   int strongBuy, int buy, int hold, int sell, int strongSell) {
        super(series);
        this.strongBuy = strongBuy;
        this.buy = buy;
        this.hold = hold;
        this.sell = sell;
        this.strongSell = strongSell;
    }

    @Override
    protected Num calculate(int index) {
        // Calculate weighted score: StrongBuy=2, Buy=1, Hold=0, Sell=-1, StrongSell=-2
        int score = (2 * strongBuy) + (1 * buy) + (0 * hold) + (-1 * sell) + (-2 * strongSell);
        return numOf(score);
    }

    public Num getScore() {
        return calculate(0);
    }

    // Helper to convert int to Num (using series' num type)
    private Num numOf(int value) {
        return getBarSeries().numOf(value);
    }

    @Override
    public int getUnstableBars() {
        return getUnstableBars();
    }
}
