package nu.itark.frosk.repo;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import nu.itark.frosk.model.FeaturedStrategy;
import nu.itark.frosk.model.StrategyTrade;

@Repository
public interface TradesRepository extends JpaRepository<StrategyTrade, Long>{
	List<StrategyTrade> findByFeaturedStrategy(FeaturedStrategy fs);
	//@Query("SELECT st FROM StrategyTrade st WHERE st.type = ?1")
	List<StrategyTrade> findTopByType(String type);
	List<StrategyTrade> findByFeaturedStrategyId(Long featuredStrategyId);
}
