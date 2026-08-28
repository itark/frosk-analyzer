package nu.itark.frosk.newsdriven;

import java.time.Instant;

/**
 * {@code id}, {@code body}, {@code sentimentScore} and {@code sourceUrl} were
 * added for the RSS pipeline ({@code NewsPoller} / persisted {@code NewsArticle}
 * rows); the original 5-arg constructor is kept for the existing Yahoo-sourced,
 * unpersisted path ({@code YahooFinanceDirectClient#getNews}), which has no id,
 * body, score or source URL of its own — those default to {@code null}/{@code 0}.
 */
public record NewsItem(
        Long id,
        String title,
        String publisher,
        String link,
        Instant publishedAt,
        String ticker,
        String body,
        int sentimentScore,
        String sourceUrl
) {
    public NewsItem(String title, String publisher, String link, Instant publishedAt, String ticker) {
        this(null, title, publisher, link, publishedAt, ticker, null, 0, null);
    }
}
