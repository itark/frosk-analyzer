package nu.itark.frosk.repo;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import nu.itark.frosk.model.FeaturedStrategy;
import nu.itark.frosk.model.StrategyTrade;

public interface TradesRepository extends JpaRepository<StrategyTrade, Long>{
	List<StrategyTrade> findByFeaturedStrategy(FeaturedStrategy fs);
	StrategyTrade findByDateAndType(Date date, String type);
	StrategyTrade findByDateAndTypeAndFeaturedStrategyId(Date date, String type, Long fsId);
	
	List<StrategyTrade> findByFeaturedStrategyId(Long featuredStrategyId);
}
