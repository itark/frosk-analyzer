package nu.itark.frosk.repo;

import nu.itark.frosk.model.IntradayBar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface IntradayBarRepository extends JpaRepository<IntradayBar, Long> {

    /**
     * All bars for a security after (exclusive) a given epoch-second cutoff,
     * ordered oldest-first so ta4j BarSeries can be built with a single pass.
     */
    List<IntradayBar> findBySecurityIdAndBarTimestampGreaterThanOrderByBarTimestampAsc(
            Long securityId, long cutoffEpochSeconds);

    List<IntradayBar> findBySecurityIdAndIntervalCodeAndBarTimestampGreaterThanOrderByBarTimestampAsc(
            Long securityId, String intervalCode, long cutoffEpochSeconds);

    List<IntradayBar> findBySecurityIdOrderByBarTimestampAsc(Long securityId);

    /**
     * Check whether a specific bar is already stored (idempotent upsert guard).
     */
    boolean existsBySecurityIdAndBarTimestampAndIntervalCode(
            Long securityId, long barTimestamp, String intervalCode);

    IntradayBar findTopBySecurityIdOrderByBarTimestampDesc(Long securityId);

    IntradayBar findTopBySecurityIdOrderByBarTimestampAsc(Long securityId);

    /**
     * Prune old bars — called at the end of every sync run to cap table growth.
     * Deletes all bars with a timestamp strictly before the supplied cutoff.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM IntradayBar b WHERE b.barTimestamp < :cutoffEpochSeconds")
    int deleteOlderThan(long cutoffEpochSeconds);
}
