package nu.itark.frosk.repo;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import nu.itark.frosk.model.FeaturedStrategy;

@Repository
public interface FeaturedStrategyRepository extends JpaRepository<FeaturedStrategy, Long>{
	List<FeaturedStrategy> findByName(String name);
	List<FeaturedStrategy> findByNameOrderByTotalProfitDesc(String name);
	FeaturedStrategy findByNameAndSecurityName(String name, String securityName);
	FeaturedStrategy findTopBySecurityNameOrderByLatestTradeDesc(String security);
	List<FeaturedStrategy> findTop10BySecurityNameOrderByLatestTradeDesc(String security);
	List<FeaturedStrategy> findTop10ByOrderByTotalProfitDesc();
	@Query("SELECT fs FROM FeaturedStrategy fs, StrategyTrade st WHERE fs.securityName = ?1 AND st.type = 'BUY'")
	List<FeaturedStrategy> findByOpenTrade(String securityName);

	@Query("SELECT fs " +
			"FROM FeaturedStrategy fs " +
			"WHERE fs.profitableTradesRatio > ?1 " +
			"AND fs.numberofTrades > ?2 " +
			"AND fs.isOpen = true " +
			"ORDER BY latestTrade DESC")
	List<FeaturedStrategy> findSmartSignals(BigDecimal profitableTradesRatio, Integer aboveNrOfTrades);

	@Query("SELECT avg(totalProfit) as totalProfit, name as name " +
			"FROM FeaturedStrategy " +
			"GROUP BY name ORDER BY avg(totalProfit) DESC")
	List<TopStrategy> findStrategies();

}
