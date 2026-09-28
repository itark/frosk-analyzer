package nu.itark.frosk.repo;

import nu.itark.frosk.model.KrakenStrategyConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KrakenStrategyConfigRepository extends JpaRepository<KrakenStrategyConfig, String> {
}
