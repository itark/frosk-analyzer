package nu.itark.frosk.repo;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import nu.itark.frosk.model.FeaturedStrategy;
import nu.itark.frosk.model.StrategyTrade;

@Repository
public interface StrategyTradeRepository extends JpaRepository<StrategyTrade, Long>{
	List<StrategyTrade> findByFeaturedStrategy(FeaturedStrategy fs);
	List<StrategyTrade> findTopByType(String type);
	List<StrategyTrade> findByFeaturedStrategyId(Long featuredStrategyId);

	@Query("SELECT sum(grossProfit) as grossProfit " +
			"FROM StrategyTrade ")
	Profit findTotalGrossProfit();

	@Query(value = "SELECT sum(gross_profit) as grossProfit " +
			"FROM Strategy_Trade "+
			"WHERE featured_strategy_id = ?1 " , nativeQuery = true)
	Profit findTotalGrossProfitForStrategy(Long featuredStrategyId);

	List<StrategyTrade> findByFeaturedStrategyIdAndDateAfter(Long featuredStrategyId, Date date);

	List<StrategyTrade> findByFeaturedStrategyIdAndDateGreaterThan(long featuredStrategyId, Date latestDate);

	/**
	 * Daily realized PnL for a strategy, across every security it trades — the
	 * series {@link nu.itark.frosk.service.DailyPnlForecastService} sends to
	 * frosk-prophet-service. Entry rows (BUY/SHRT) carry {@code pnl=null} and
	 * are excluded; only closing rows (SELL/COVR) have a realized value.
	 * {@code featured_strategy} is keyed by (strategy, security), so this
	 * joins across all securities the named strategy has ever traded.
	 */
	@Query(value = "SELECT CAST(st.date AS DATE) AS trade_date, SUM(st.pnl) AS daily_pnl " +
			"FROM strategy_trade st " +
			"JOIN featured_strategy fs ON fs.id = st.featured_strategy_id " +
			"WHERE fs.name = ?1 AND st.pnl IS NOT NULL " +
			"GROUP BY CAST(st.date AS DATE) " +
			"ORDER BY trade_date", nativeQuery = true)
	List<DailyPnlRow> findDailyPnlByStrategyName(String strategyName);
}
