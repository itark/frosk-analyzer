package nu.itark.frosk.strategies.indicators;

import nu.itark.frosk.newsdriven.NewsService;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;

/**
 * Boolean indicator that is {@code true} when there is qualifying positive news
 * for the given ticker published within the configured time window.
 *
 * <p>Uses {@link NewsService} for in-memory cached news with sentiment scoring.
 * The indicator fetches fresh news on every call (respecting the service's own
 * 15-minute re-fetch interval), so it reflects the current market state rather
 * than historical bar data.
 */
public class NewsIndicator extends CachedIndicator<Boolean> {

    private final NewsService newsService;
    private final String ticker;
    private final int minScore;
    private final int withinMinutes;

    public NewsIndicator(BarSeries series, NewsService newsService,
                         String ticker, int minScore, int withinMinutes) {
        super(series);
        this.newsService = newsService;
        this.ticker = ticker;
        this.minScore = minScore;
        this.withinMinutes = withinMinutes;
    }

    @Override
    protected Boolean calculate(int index) {
        // News is a real-time signal, not a historical one. Returning true for past
        // bars would make the backtest replay enter at bar 0 whenever news happens to
        // be positive *now*, causing ghost SELL signals without matching BUYs.
        // Only the current (last) bar reflects live market state.
        if (index != getBarSeries().getEndIndex()) {
            return false;
        }
        newsService.fetchAndCacheNews(ticker);
        return newsService.hasPositiveNews(ticker, minScore, withinMinutes);
    }

    @Override
    public int getUnstableBars() {
        return 0;
    }
}
