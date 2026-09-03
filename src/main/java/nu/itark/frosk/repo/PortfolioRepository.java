package nu.itark.frosk.repo;

import nu.itark.frosk.model.Portfolio;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    Optional<Portfolio> findTopByOrderBySnapshotDateDesc();

    Optional<Portfolio> findTopByPortfolioTypeOrderBySnapshotDateDesc(String portfolioType);

    /** Returns true if at least one portfolio snapshot was saved within the given time window. */
    boolean existsBySnapshotDateBetween(Date start, Date end);

    boolean existsByPortfolioTypeAndSnapshotDateBetween(String portfolioType, Date start, Date end);

    List<Portfolio> findByPortfolioTypeOrderBySnapshotDateDesc(String portfolioType);

    /** Newest-first, capped to {@code pageable}'s page size — used by the history endpoints
     *  so the intraday portfolio (a snapshot every 15 min) doesn't return thousands of rows. */
    List<Portfolio> findByPortfolioTypeOrderBySnapshotDateDesc(String portfolioType, Pageable pageable);
}
