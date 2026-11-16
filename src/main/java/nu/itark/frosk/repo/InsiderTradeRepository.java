package nu.itark.frosk.repo;

import nu.itark.frosk.model.InsiderTrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface InsiderTradeRepository extends JpaRepository<InsiderTrade, Long> {

    boolean existsByExternalId(String externalId);

    List<InsiderTrade> findByTransactionTypeAndTransactionDateAfter(String transactionType, Date date);
}
