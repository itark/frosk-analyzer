package nu.itark.frosk.newsdriven;

import java.time.Instant;

public record NewsItem(
        String title,
        String publisher,
        String link,
        Instant publishedAt,
        String ticker
) {}
