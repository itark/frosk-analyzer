package nu.itark.frosk.bot.bot.repository;

import nu.itark.frosk.bot.bot.domain.ImportedTicker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * {@link ImportedTicker} repository.
 */
@Repository
public interface ImportedTickerRepository extends JpaRepository<ImportedTicker, Long>, JpaSpecificationExecutor<ImportedTicker> {

    /**
     * Returns imported tickers (ordered by timestamp).
     *
     * @return imported tickers
     */
    List<ImportedTicker> findByOrderByTimestampAsc();

    /**
     * Returns imported tickers of a specific currency pair (ordered by timestamp).
     *
     * @param currencyPair currency pair
     * @return imported tickers
     */
    List<ImportedTicker> findByCurrencyPairOrderByTimestampAsc(String currencyPair);

}
