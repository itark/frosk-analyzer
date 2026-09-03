package nu.itark.frosk.repo;

import nu.itark.frosk.model.KrakenFuturesPaperOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface KrakenFuturesPaperOrderRepository extends JpaRepository<KrakenFuturesPaperOrder, Long> {

    /** Most recent OPEN position for the given (symbol, strategy, direction). */
    Optional<KrakenFuturesPaperOrder> findTopBySymbolAndStrategyNameAndDirectionAndStatusOrderByCreatedAtDesc(
            String symbol, String strategyName, String direction, String status);

    /** All currently open positions (any direction). */
    List<KrakenFuturesPaperOrder> findByStatusOrderByCreatedAtDesc(String status);

    /** USD notional of all currently open positions (used for exposure cap). */
    @Query(value = "SELECT COALESCE(SUM(usd_amount), 0) FROM kraken_futures_paper_order " +
                   "WHERE status = 'OPEN'",
           nativeQuery = true)
    BigDecimal sumOpenExposureUsd();
}
