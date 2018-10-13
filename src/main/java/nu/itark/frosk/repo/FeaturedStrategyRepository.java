package nu.itark.frosk.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import nu.itark.frosk.model.FeaturedStrategy;

public interface FeaturedStrategyRepository extends JpaRepository<FeaturedStrategy, Long>{
	List<FeaturedStrategy> findByName(String name);
	List<FeaturedStrategy> findByNameOrderByTotalProfitDesc(String name);
	FeaturedStrategy findByNameAndSecurityName(String name, String securityName);
	FeaturedStrategy findTopBySecurityNameOrderByLatestTradeDesc(String security);
//	List<FeaturedStrategy> findByNameAndDataSet(String name, DataSet dataset);
	
}
