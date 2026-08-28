package nu.itark.frosk.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * One row per RSS news item polled from Nasdaq Nordic, with the ticker it was
 * matched to (if any) and its keyword-based sentiment score.
 *
 * <p>{@code sourceUrl} is unique and is how {@code NewsPoller} deduplicates
 * against already-seen items across polling cycles.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "news_article")
public class NewsArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "published_at", nullable = false)
    private Instant publishedAt;

    /** Matched security ticker (e.g. {@code VOLV-B.ST}), or null when no known ticker matched. */
    @Column(name = "ticker")
    private String ticker;

    @Column(name = "headline", nullable = false, length = 1000)
    private String headline;

    @Column(name = "body", length = 8000)
    private String body;

    @Column(name = "sentiment_score", nullable = false)
    private int sentimentScore;

    @Column(name = "source_url", unique = true, length = 500)
    private String sourceUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
