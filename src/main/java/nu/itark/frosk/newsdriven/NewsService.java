package nu.itark.frosk.newsdriven;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.dataset.YahooFinanceDirectClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory news cache with sentiment scoring for intraday strategy signals.
 *
 * <p>News is fetched from Yahoo Finance via {@link YahooFinanceDirectClient},
 * scored by {@link KeywordSentimentAnalyzer}, and cached per ticker with a
 * 60-minute TTL. The cache is refreshed at most once per 15 minutes per ticker
 * to avoid excessive API calls during the intraday runner's 10-minute cycle.
 */
@Service
@Slf4j
public class NewsService {

    private static final Duration CACHE_TTL       = Duration.ofMinutes(60);
    private static final Duration FETCH_INTERVAL  = Duration.ofMinutes(15);

    record CachedEntry(NewsItem item, int score, Instant fetchedAt) {}

    private final ConcurrentHashMap<String, List<CachedEntry>> newsCache    = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Instant>           lastFetchAt  = new ConcurrentHashMap<>();

    @Autowired
    private YahooFinanceDirectClient yahooClient;

    @Autowired
    private KeywordSentimentAnalyzer sentimentAnalyzer;

    /**
     * Fetches and caches news for a ticker if the cache is stale (> 15 min).
     * Call this before {@link #hasPositiveNews} to ensure fresh data.
     */
    public void fetchAndCacheNews(String ticker) {
        Instant now = Instant.now();
        Instant last = lastFetchAt.getOrDefault(ticker, Instant.EPOCH);
        if (Duration.between(last, now).compareTo(FETCH_INTERVAL) < 0) {
            return;
        }

        List<NewsItem> items = yahooClient.getNews(ticker);
        List<CachedEntry> entries = items.stream()
                .map(item -> new CachedEntry(item, sentimentAnalyzer.analyze(item.title()), now))
                .toList();

        newsCache.put(ticker, entries);
        lastFetchAt.put(ticker, now);
        log.debug("NewsService: cached {} items for {} (scores: {})",
                entries.size(), ticker,
                entries.stream().map(CachedEntry::score).toList());
    }

    /**
     * Returns {@code true} if there is at least one news item with
     * {@code score >= minScore} published within {@code withinMinutes} minutes.
     */
    public boolean hasPositiveNews(String ticker, int minScore, int withinMinutes) {
        Instant cutoff = Instant.now().minus(Duration.ofMinutes(withinMinutes));
        List<CachedEntry> entries = newsCache.getOrDefault(ticker, Collections.emptyList());
        List<CachedEntry> recent = entries.stream()
                .filter(e -> e.item().publishedAt().isAfter(cutoff))
                .toList();
        boolean result = recent.stream().anyMatch(e -> e.score() >= minScore);
        if (!recent.isEmpty()) {
            log.info("NewsService[{}]: {} recent items (cutoff -{} min), qualifying={}, scores={}",
                    ticker, recent.size(), withinMinutes, result,
                    recent.stream().map(e -> e.score() + ":\"" + e.item().title() + "\"").toList());
        } else {
            log.debug("NewsService[{}]: no items within {} min (cache size={})",
                    ticker, withinMinutes, entries.size());
        }
        return result;
    }

    /**
     * Returns the highest sentiment score among news published within 60 minutes,
     * or {@link Integer#MIN_VALUE} if no recent news exists.
     */
    public int getLatestScore(String ticker) {
        Instant cutoff = Instant.now().minus(CACHE_TTL);
        List<CachedEntry> entries = newsCache.getOrDefault(ticker, Collections.emptyList());
        return entries.stream()
                .filter(e -> e.item().publishedAt().isAfter(cutoff))
                .mapToInt(CachedEntry::score)
                .max()
                .orElse(Integer.MIN_VALUE);
    }
}
