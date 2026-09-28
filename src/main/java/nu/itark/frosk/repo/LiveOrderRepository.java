package nu.itark.frosk.repo;

import nu.itark.frosk.model.LiveOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface LiveOrderRepository extends JpaRepository<LiveOrder, Long> {

    List<LiveOrder> findByTickerAndSideAndStatusAndCreatedAtAfter(
            String ticker, String side, String status, LocalDateTime since);

    Optional<LiveOrder> findTopByTickerAndStrategyNameAndSideAndStatusOrderByCreatedAtDesc(
            String ticker, String strategyName, String side, String status);

    List<LiveOrder> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime since);

    long countByCreatedAtAfter(LocalDateTime since);

    /** Sum of realized EUR losses (negative PnL rows) since midnight UTC. */
    @Query(value = "SELECT COALESCE(SUM(realized_pnl_eur), 0) FROM live_order " +
                   "WHERE realized_pnl_eur < 0 AND created_at >= :since",
           nativeQuery = true)
    BigDecimal sumEurLossSince(@Param("since") LocalDateTime since);

    /** Total realized PnL (gains + losses) since midnight UTC. */
    @Query(value = "SELECT COALESCE(SUM(realized_pnl_eur), 0) FROM live_order " +
                   "WHERE realized_pnl_eur IS NOT NULL AND created_at >= :since",
           nativeQuery = true)
    BigDecimal sumRealizedPnlSince(@Param("since") LocalDateTime since);

    /**
     * Cost basis of every position that is, or may be, open: long AND short
     * entries (short entries were previously missing, so the total-exposure cap
     * ignored them), including Kraken entries whose fill is still unconfirmed or
     * whose exit is in flight — counted conservatively, since the money may
     * already be committed.
     */
    @Query(value = "SELECT COALESCE(SUM(eur_amount), 0) FROM live_order " +
                   "WHERE side IN ('BUY', 'SHRT') AND status IN ('FILLED', 'PENDING', 'CLOSING', 'UNRESOLVED')",
           nativeQuery = true)
    BigDecimal sumOpenExposureEur();

    long countByStatus(String status);

    /**
     * Count of positions that are, or may be, open — same WHERE clause as
     * {@link #sumOpenExposureEur()}, just COUNT instead of SUM. Backs {@code
     * LiveTradingGate}'s max-open-positions check, the live-side mirror of
     * {@code RiskManagementService}'s paper-only {@code risk.max.open.positions}
     * (added 2026-09-28 to close that config-parity gap).
     */
    @Query(value = "SELECT COUNT(*) FROM live_order " +
                   "WHERE side IN ('BUY', 'SHRT') AND status IN ('FILLED', 'PENDING', 'CLOSING', 'UNRESOLVED')",
           nativeQuery = true)
    long countOpenPositions();

    /** Open entries guarded by a resting stop — checked for stop fills by the reconciler. */
    List<LiveOrder> findByStatusAndProtectiveStopOrderIdIsNotNull(String status);

    /** Orders awaiting fill confirmation — the Kraken reconciler's work queue. */
    List<LiveOrder> findByStatusOrderByCreatedAtAsc(String status);

    /** Entries on {@code ticker} in the given sides/statuses — used to refuse opposite-direction entries. */
    List<LiveOrder> findByTickerAndSideInAndStatusIn(String ticker, Collection<String> sides, Collection<String> statuses);

    /** Most recent entry of a (ticker, strategy, side) in any of {@code statuses}. */
    Optional<LiveOrder> findTopByTickerAndStrategyNameAndSideAndStatusInOrderByCreatedAtDesc(
            String ticker, String strategyName, String side, Collection<String> statuses);
}
