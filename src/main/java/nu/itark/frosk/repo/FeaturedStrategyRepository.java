package nu.itark.frosk.repo;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import nu.itark.frosk.model.FeaturedStrategy;

public interface FeaturedStrategyRepository extends CrudRepository<FeaturedStrategy, Long>{
	List<FeaturedStrategy> findByName(String name);
	List<FeaturedStrategy> findByNameOrderByTotalProfitDesc(String name);
	List<FeaturedStrategy> findBySecurity(String security);
	FeaturedStrategy findTopBySecurityOrderByLatestTradeDesc(String security);
	FeaturedStrategy findByNameAndSecurity(String name, String security);
	
}
