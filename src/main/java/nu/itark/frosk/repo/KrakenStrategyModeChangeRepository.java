package nu.itark.frosk.repo;

import nu.itark.frosk.model.KrakenStrategyModeChange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KrakenStrategyModeChangeRepository extends JpaRepository<KrakenStrategyModeChange, Long> {

    List<KrakenStrategyModeChange> findByStrategyNameOrderByChangedAtDesc(String strategyName);
}
