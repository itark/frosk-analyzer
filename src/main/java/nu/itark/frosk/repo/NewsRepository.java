package nu.itark.frosk.repo;

import nu.itark.frosk.model.NewsArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Persistence for {@link NewsArticle} rows polled by {@code NewsPoller}.
 *
 * <p>Named {@code NewsRepository} rather than {@code NewsArticleRepository}
 * because the entity it manages is the persisted counterpart of the
 * {@code newsdriven.NewsItem} value object — {@code NewsArticle} exists as a
 * distinct JPA-mapped class (Lombok, mutable, {@code @Entity}, per
 * {@code .claude/rules/persistence.md}) rather than {@code NewsItem} itself,
 * because {@code NewsItem} is a record and this Spring Boot/Hibernate version
 * (6.2.x) does not support Java records as full mutable {@code @Entity}
 * types — only as embeddables/DTO projections.
 */
@Repository
public interface NewsRepository extends JpaRepository<NewsArticle, Long> {

    boolean existsBySourceUrl(String sourceUrl);

    List<NewsArticle> findByTickerAndPublishedAtAfterOrderByPublishedAtDesc(String ticker, Instant after);

    List<NewsArticle> findByTickerAndPublishedAtBetween(String ticker, Instant from, Instant to);
}
