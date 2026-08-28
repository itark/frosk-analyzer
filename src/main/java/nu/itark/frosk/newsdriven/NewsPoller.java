package nu.itark.frosk.newsdriven;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.model.NewsArticle;
import nu.itark.frosk.repo.NewsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Polls the Nasdaq Nordic RSS feed every 30s, scores each new item's
 * sentiment with the existing {@link KeywordSentimentAnalyzer} and matches it
 * to a known ticker with {@link TickerExtractor}, then persists it (via
 * {@link NewsRepository}) for {@code strategies.indicators.NewsIndicator} to
 * read back. Deduplicates on {@code sourceUrl} (the item's GUID, or its link
 * when no GUID is present).
 *
 * <p>Each parsed entry is first assembled as a {@link NewsItem} — using the
 * fields added for this pipeline ({@code body}, {@code sentimentScore},
 * {@code sourceUrl}) — and then mapped to the persisted {@link NewsArticle}
 * row; {@link NewsItem} stays the shared in-memory shape for both this and
 * the existing Yahoo-sourced path in {@link NewsService}.
 *
 * <p>Disabled in the crypto profile — Nasdaq Nordic is Swedish equity news,
 * irrelevant to the crypto process — via {@code frosk.newsdriven.rss.poll.enabled=false}
 * in {@code application-crypto.properties}. Defaults to enabled, matching the
 * equity profile and the no-profile-given default (see CLAUDE.md).
 */
@Component
@Slf4j
@ConditionalOnProperty(name = "frosk.newsdriven.rss.poll.enabled", havingValue = "true", matchIfMissing = true)
public class NewsPoller {

    private static final String FEED_URL = "https://api.news.eu.nasdaq.com/news/rss/nasdaqNordicNews";
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 10_000;

    @Autowired
    private NewsRepository newsRepository;

    @Autowired
    private TickerExtractor tickerExtractor;

    @Autowired
    private KeywordSentimentAnalyzer sentimentAnalyzer;

    @Scheduled(fixedDelay = 30_000)
    public void poll() {
        List<SyndEntry> entries;
        try {
            entries = fetchFeed();
        } catch (Exception e) {
            log.warn("NewsPoller: failed to fetch/parse Nasdaq Nordic RSS: {}", e.toString());
            return;
        }

        int saved = 0;
        for (SyndEntry entry : entries) {
            String sourceUrl = Optional.ofNullable(entry.getUri()).filter(s -> !s.isBlank())
                    .orElse(entry.getLink());
            if (sourceUrl == null || sourceUrl.isBlank()) {
                continue;
            }
            if (newsRepository.existsBySourceUrl(sourceUrl)) {
                continue;
            }

            NewsItem newsItem = toNewsItem(entry, sourceUrl);
            newsRepository.save(toEntity(newsItem));
            saved++;
        }
        if (saved > 0) {
            log.info("NewsPoller: saved {} new article(s) of {} fetched", saved, entries.size());
        }
    }

    private NewsItem toNewsItem(SyndEntry entry, String sourceUrl) {
        String headline = entry.getTitle();
        String body = entry.getDescription() != null ? entry.getDescription().getValue() : null;
        String combined = headline + (body != null ? " " + body : "");
        Instant publishedAt = entry.getPublishedDate() != null
                ? entry.getPublishedDate().toInstant() : Instant.now();
        String ticker = tickerExtractor.extract(combined).orElse(null);
        int score = sentimentAnalyzer.analyze(combined);

        return new NewsItem(null, headline, entry.getAuthor(), entry.getLink(),
                publishedAt, ticker, body, score, sourceUrl);
    }

    private NewsArticle toEntity(NewsItem item) {
        NewsArticle article = new NewsArticle();
        article.setHeadline(item.title());
        article.setBody(item.body());
        article.setPublishedAt(item.publishedAt());
        article.setTicker(item.ticker());
        article.setSentimentScore(item.sentimentScore());
        article.setSourceUrl(item.sourceUrl());
        return article;
    }

    private List<SyndEntry> fetchFeed() throws Exception {
        URL url = URI.create(FEED_URL).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setRequestProperty("User-Agent", "frosk-analyzer/1.0");
        try (XmlReader reader = new XmlReader(connection.getInputStream(), connection.getContentType())) {
            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(reader);
            return feed.getEntries();
        }
    }
}
