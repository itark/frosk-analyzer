package nu.itark.frosk.repo;

import nu.itark.frosk.model.IntradaySignal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface IntradaySignalRepository extends JpaRepository<IntradaySignal, Long> {

    List<IntradaySignal> findByTickerAndSignalTimestampGreaterThanOrderBySignalTimestampDesc(
            String ticker, long cutoffEpochSeconds);

    List<IntradaySignal> findTop20ByOrderBySignalTimestampDesc();

    List<IntradaySignal> findByTickerAndStrategyNameOrderBySignalTimestampAsc(
            String ticker, String strategyName);

    List<IntradaySignal> findByTickerAndStrategyNameAndSignalTimestampGreaterThanEqualOrderBySignalTimestampAsc(
            String ticker, String strategyName, long fromEpochSeconds);

    @Query("SELECT DISTINCT s.ticker FROM IntradaySignal s WHERE s.ticker IS NOT NULL")
    List<String> findDistinctTickers();

    @Query("SELECT DISTINCT s.strategyName FROM IntradaySignal s WHERE s.strategyName IS NOT NULL")
    List<String> findDistinctStrategyNames();

    boolean existsByStrategyNameAndTickerAndSignalTimestampAndSignalType(
            String strategyName, String ticker, long signalTimestamp, String signalType);

    Optional<IntradaySignal> findTopByStrategyNameAndTickerAndSignalTypeOrderBySignalTimestampDesc(
            String strategyName, String ticker, String signalType);

    boolean existsByStrategyNameAndTickerAndSignalTypeAndSignalTimestampGreaterThan(
            String strategyName, String ticker, String signalType, long afterTimestamp);
}
