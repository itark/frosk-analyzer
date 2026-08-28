package nu.itark.frosk.strategies.indicators;

import nu.itark.frosk.model.NewsArticle;
import nu.itark.frosk.repo.NewsRepository;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Boolean indicator that is {@code true} when a persisted RSS {@link NewsArticle}
 * with {@code |sentimentScore| >= minAbsSentimentScore} exists for the given
 * ticker within {@code lookbackMinutes} of the bar's end time.
 *
 * <p>Backed by {@link NewsRepository} (rows written by {@code NewsPoller}
 * polling Nasdaq Nordic RSS), so — unlike an in-memory, real-time-only cache
 * would be — this evaluates correctly for every bar in a backtest, not just
 * the latest one: each bar looks back at the news that existed as of that
 * bar's own timestamp.
 *
 * <p>Fails closed (returns {@code false}) when no ticker could be resolved
 * for the series — a missing ticker must never be treated as "news present".
 */
public class NewsIndicator extends CachedIndicator<Boolean> {

    private final NewsRepository newsRepository;
    private final String ticker;
    private final int lookbackMinutes;
    private final int minAbsSentimentScore;

    public NewsIndicator(BarSeries series, NewsRepository newsRepository,
                          String ticker, int lookbackMinutes, int minAbsSentimentScore) {
        super(series);
        this.newsRepository = newsRepository;
        this.ticker = ticker;
        this.lookbackMinutes = lookbackMinutes;
        this.minAbsSentimentScore = minAbsSentimentScore;
    }

    @Override
    protected Boolean calculate(int index) {
        if (ticker == null || ticker.isBlank()) {
            return false;
        }
        Instant barTime = getBarSeries().getBar(index).getEndTime().toInstant();
        Instant cutoff = barTime.minus(Duration.ofMinutes(lookbackMinutes));

        List<NewsArticle> recent = newsRepository.findByTickerAndPublishedAtBetween(ticker, cutoff, barTime);
        return recent.stream().anyMatch(a -> Math.abs(a.getSentimentScore()) >= minAbsSentimentScore);
    }

    @Override
    public int getUnstableBars() {
        return 0;
    }
}
