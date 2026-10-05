package nu.itark.frosk.repo;

import nu.itark.frosk.model.CoinbaseStrategyModeChange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CoinbaseStrategyModeChangeRepository extends JpaRepository<CoinbaseStrategyModeChange, Long> {

    List<CoinbaseStrategyModeChange> findByStrategyNameOrderByChangedAtDesc(String strategyName);
}
