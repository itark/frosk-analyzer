package nu.itark.frosk.repo;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import nu.itark.frosk.model.FeaturedStrategy;
import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.model.StrategyTrade;

public interface StrategyIndicatorValueRepository extends JpaRepository<StrategyIndicatorValue, Long>{
	List<StrategyIndicatorValue> findByFeaturedStrategy(FeaturedStrategy fs);
	List<StrategyIndicatorValue> findByFeaturedStrategyOrderByDate(FeaturedStrategy fs);

	StrategyIndicatorValue findByDate(Date date);
	StrategyIndicatorValue findByDateAndFeaturedStrategyId(Date date, Long fsId);
	
	List<StrategyIndicatorValue> findByFeaturedStrategyId(Long featuredStrategyId);
}
