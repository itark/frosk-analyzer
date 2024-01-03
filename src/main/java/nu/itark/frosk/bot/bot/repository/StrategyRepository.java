package nu.itark.frosk.bot.bot.repository;

import nu.itark.frosk.bot.bot.domain.Strategy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * {@link Strategy} repository.
 */
@Repository
public interface StrategyRepository extends JpaRepository<Strategy, Long>, JpaSpecificationExecutor<Strategy> {

    /**
     * Find a strategy by its strategy id.
     *
     * @param strategyId strategy id
     * @return strategy
     */
    Optional<Strategy> findByStrategyId(String strategyId);

}
