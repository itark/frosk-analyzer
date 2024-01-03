package nu.itark.frosk.bot.bot.repository;

import nu.itark.frosk.bot.bot.domain.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * {@link Trade} repository.
 */
@Repository
public interface TradeRepository extends JpaRepository<Trade, Long>, JpaSpecificationExecutor<Trade> {

    /**
     * Find a trade by its trade id.
     *
     * @param tradeId trade id
     * @return trade
     */
    Optional<Trade> findByTradeId(String tradeId);

    /**
     * Retrieve all trades (sorted by timestamp).
     *
     * @return trades
     */
    List<Trade> findByOrderByTimestampAsc();

}
