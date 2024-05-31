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

	@Query("SELECT fs " +
			"FROM FeaturedStrategy fs " +
			"WHERE fs.profitableTradesRatio > ?1 " +
			"AND fs.numberofTrades > ?2 " +
			"AND fs.sqn > ?3 " +
			"AND fs.expectency > ?4 " +
			"AND fs.isOpen = ?5 " +
			"ORDER BY totalProfit DESC")
	List<FeaturedStrategy> findTopStrategies(BigDecimal profitableTradesRatio, Integer nrOfTrades, BigDecimal sqn, BigDecimal expectency, Boolean isOpen);

	@Query("SELECT fs " +
			"FROM FeaturedStrategy fs " +
			"WHERE fs.profitableTradesRatio > ?1 " +
			"AND fs.numberofTrades > ?2 " +
			"AND fs.sqn > ?3 " +
			"AND fs.expectency > ?4 " +
			"AND fs.isOpen = ?5 " +
			"ORDER BY latestTrade DESC")
	List<FeaturedStrategy> findSmartSignals(BigDecimal profitableTradesRatio, Integer nrOfTrades, BigDecimal sqn, BigDecimal expectency, Boolean isOpen);

	@Query("SELECT avg(sqn) as sqn, avg(totalProfit) as totalProfit, name as name " +
			"FROM FeaturedStrategy " +
			"GROUP BY name")
	List<TopStrategy> findBestPerformingStrategies();

}
