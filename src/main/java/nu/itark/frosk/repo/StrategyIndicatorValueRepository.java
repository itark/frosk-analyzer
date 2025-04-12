package nu.itark.frosk.repo;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import nu.itark.frosk.model.FeaturedStrategy;
import nu.itark.frosk.model.StrategyIndicatorValue;
import nu.itark.frosk.model.StrategyTrade;

@Repository
public interface StrategyIndicatorValueRepository extends JpaRepository<StrategyIndicatorValue, Long>{
	List<StrategyIndicatorValue> findByFeaturedStrategyId(Long featuredStrategyId);
}
