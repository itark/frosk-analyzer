package nu.itark.frosk.repo;

import nu.itark.frosk.model.IntradaySignal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface IntradaySignalRepository extends JpaRepository<IntradaySignal, Long> {

    /**
     * All signals for a ticker after a given epoch-second cutoff,
     * newest first — used by the REST watchlist endpoint and H2 queries.
     */
    List<IntradaySignal> findByTickerAndSignalTimestampGreaterThanOrderBySignalTimestampDesc(
            String ticker, long cutoffEpochSeconds);

    /**
     * Latest N signals across all tickers — for the dashboard endpoint.
     */
    List<IntradaySignal> findTop20ByOrderBySignalTimestampDesc();

    /**
     * All signals for a ticker, oldest first — used for PnL round-trip pairing.
     */
    List<IntradaySignal> findByTickerOrderBySignalTimestampAsc(String ticker);

    @Query("SELECT DISTINCT s.ticker FROM IntradaySignal s")
    List<String> findDistinctTickers();

    /**
     * Guard against duplicate signals: did we already record this exact bar?
     */
    boolean existsByTickerAndSignalTimestampAndSignalType(
            String ticker, long signalTimestamp, String signalType);
}
