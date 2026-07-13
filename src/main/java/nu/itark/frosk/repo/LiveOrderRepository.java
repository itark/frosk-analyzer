package nu.itark.frosk.repo;

import nu.itark.frosk.model.LiveOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    /** EUR cost basis of all currently open positions (filled BUYs not yet matched by a SELL). */
    @Query(value = "SELECT COALESCE(SUM(eur_amount), 0) FROM live_order " +
                   "WHERE side = 'BUY' AND status = 'FILLED'",
           nativeQuery = true)
    BigDecimal sumOpenExposureEur();
}
