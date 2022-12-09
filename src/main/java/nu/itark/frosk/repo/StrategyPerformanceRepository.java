package nu.itark.frosk.repo;

import nu.itark.frosk.model.FeaturedStrategy;
import nu.itark.frosk.model.StrategyPerformance;
import nu.itark.frosk.model.StrategyTrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface StrategyPerformanceRepository extends JpaRepository<StrategyPerformance, Long>{
    List<StrategyPerformance> findBySecurityNameAndDate(String securityName, Date date);
}
