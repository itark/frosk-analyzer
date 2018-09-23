package nu.itark.frosk.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import nu.itark.frosk.model.FeaturedStrategy;
import nu.itark.frosk.model.Security;

public interface FeaturedStrategyRepository extends JpaRepository<FeaturedStrategy, Long>{
	List<FeaturedStrategy> findByName(String name);
	List<FeaturedStrategy> findByNameOrderByTotalProfitDesc(String name);
	List<FeaturedStrategy> findBySecurityName(String security);
	FeaturedStrategy findTopBySecurityNameOrderByLatestTradeDesc(String security);
	FeaturedStrategy findByNameAndSecurity(String name, Security security);
	
}
