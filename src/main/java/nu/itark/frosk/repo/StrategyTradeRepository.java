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

}
