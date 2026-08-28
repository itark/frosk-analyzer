package nu.itark.frosk.repo;

import nu.itark.frosk.model.OrderBookMinuteSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface OrderBookMinuteSnapshotRepository extends JpaRepository<OrderBookMinuteSnapshot, Long> {

    boolean existsBySecurityIdAndMinuteTimestamp(Long securityId, long minuteTimestamp);

    OrderBookMinuteSnapshot findTopBySecurityIdOrderByMinuteTimestampDesc(Long securityId);

    /** Ascending — the order the rolling 15-minute OFI sum and any backtest need. */
    List<OrderBookMinuteSnapshot> findBySecurityIdAndMinuteTimestampGreaterThanEqualOrderByMinuteTimestampAsc(
            Long securityId, long fromEpochSeconds);

    List<OrderBookMinuteSnapshot> findBySecurityIdAndMinuteTimestampBetweenOrderByMinuteTimestampAsc(
            Long securityId, long fromEpochSeconds, long toEpochSeconds);

    /**
     * Prune old rows — mirrors {@code IntradayBarRepository}'s retention pattern.
     * At 1-minute granularity even a modest product count accumulates fast, and
     * this table has no reason to keep more history than the pre-registration's
     * evaluation window needs.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM OrderBookMinuteSnapshot s WHERE s.minuteTimestamp < :cutoffEpochSeconds")
    int deleteOlderThan(long cutoffEpochSeconds);
}
