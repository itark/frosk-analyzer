package nu.itark.frosk.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import nu.itark.frosk.model.FeaturedStrategy;

public interface FeaturedStrategyRepository extends JpaRepository<FeaturedStrategy, Long>{
	List<FeaturedStrategy> findByName(String name);
	List<FeaturedStrategy> findByNameOrderByTotalProfitDesc(String name);
	List<FeaturedStrategy> findBySecurityName(String security);
	FeaturedStrategy findTopBySecurityNameOrderByLatestTradeDesc(String security);
	FeaturedStrategy findByNameAndSecurityName(String name, String security);
	
}
