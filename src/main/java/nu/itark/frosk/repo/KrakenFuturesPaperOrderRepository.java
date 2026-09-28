package nu.itark.frosk.repo;

import nu.itark.frosk.model.KrakenFuturesPaperOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface KrakenFuturesPaperOrderRepository extends JpaRepository<KrakenFuturesPaperOrder, Long> {

    /** Most recent OPEN position for the given (symbol, strategy, direction). */
    Optional<KrakenFuturesPaperOrder> findTopBySymbolAndStrategyNameAndDirectionAndStatusOrderByCreatedAtDesc(
            String symbol, String strategyName, String direction, String status);

    /** A strategy's closed positions in close order — the trade list behind the promotion metrics. */
    List<KrakenFuturesPaperOrder> findByStrategyNameAndStatusOrderByClosedAtAsc(String strategyName, String status);

    /** A strategy's first-ever paper position — fallback start date when it has no signals recorded. */
    Optional<KrakenFuturesPaperOrder> findTopByStrategyNameOrderByCreatedAtAsc(String strategyName);

    /** All currently open positions (any direction). */
    List<KrakenFuturesPaperOrder> findByStatusOrderByCreatedAtDesc(String status);

    /** Count of currently open positions — used by RiskManagementService's max-open-positions check. */
    long countByStatus(String status);

    /** USD notional of all currently open positions (used for exposure cap). */
    @Query(value = "SELECT COALESCE(SUM(usd_amount), 0) FROM kraken_futures_paper_order " +
                   "WHERE status = 'OPEN'",
           nativeQuery = true)
    BigDecimal sumOpenExposureUsd();

    /**
     * Realized PnL (USD) of positions closed at or after {@code cutoff} — used by
     * RiskManagementService's daily-loss circuit breaker. {@code cutoff} is the
     * caller's start-of-day instant (UTC).
     */
    @Query(value = "SELECT COALESCE(SUM(realized_pnl_usd), 0) FROM kraken_futures_paper_order " +
                   "WHERE status = 'CLOSED' AND closed_at >= ?1",
           nativeQuery = true)
    BigDecimal sumRealizedPnlUsdSince(LocalDateTime cutoff);
}
