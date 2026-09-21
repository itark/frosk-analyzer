package nu.itark.frosk.repo;

import java.math.BigDecimal;
import java.sql.Date;

/** One day's aggregated realized PnL for a strategy — see {@link StrategyTradeRepository#findDailyPnlByStrategyName}. */
public interface DailyPnlRow {
    Date getTradeDate();
    BigDecimal getDailyPnl();
}
