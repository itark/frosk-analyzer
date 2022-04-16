package nu.itark.frosk.repo;

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
	@Query("SELECT fs FROM FeaturedStrategy fs, StrategyTrade st WHERE fs.securityName = ?1 AND st.type = 'BUY'")
	List<FeaturedStrategy> findByOpenTrade(String securityName);

}
