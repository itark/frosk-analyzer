package nu.itark.frosk.repo;

import nu.itark.frosk.model.CoinbaseStrategyConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CoinbaseStrategyConfigRepository extends JpaRepository<CoinbaseStrategyConfig, String> {
}
