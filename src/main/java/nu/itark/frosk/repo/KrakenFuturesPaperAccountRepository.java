package nu.itark.frosk.repo;

import nu.itark.frosk.model.KrakenFuturesPaperAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KrakenFuturesPaperAccountRepository extends JpaRepository<KrakenFuturesPaperAccount, Long> {
}
