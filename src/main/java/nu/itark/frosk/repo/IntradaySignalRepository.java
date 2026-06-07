package nu.itark.frosk.repo;

import nu.itark.frosk.model.IntradaySignal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface IntradaySignalRepository extends JpaRepository<IntradaySignal, Long> {

    List<IntradaySignal> findByTickerAndSignalTimestampGreaterThanOrderBySignalTimestampDesc(
            String ticker, long cutoffEpochSeconds);

    List<IntradaySignal> findTop20ByOrderBySignalTimestampDesc();

    List<IntradaySignal> findByTickerAndStrategyNameOrderBySignalTimestampAsc(
            String ticker, String strategyName);

    @Query("SELECT DISTINCT s.ticker FROM IntradaySignal s")
    List<String> findDistinctTickers();

    @Query("SELECT DISTINCT s.strategyName FROM IntradaySignal s")
    List<String> findDistinctStrategyNames();

    boolean existsByStrategyNameAndTickerAndSignalTimestampAndSignalType(
            String strategyName, String ticker, long signalTimestamp, String signalType);
}
