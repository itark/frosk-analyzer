package nu.itark.frosk.repo;

import nu.itark.frosk.model.CryptoPaperOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface CryptoPaperOrderRepository extends JpaRepository<CryptoPaperOrder, Long> {

    Optional<CryptoPaperOrder> findTopByTickerAndStrategyNameAndSideAndStatusOrderByCreatedAtDesc(
            String ticker, String strategyName, String side, String status);

    List<CryptoPaperOrder> findBySideAndStatusOrderByCreatedAtDesc(String side, String status);

    /** EUR cost basis of all currently open paper positions (filled BUYs not yet matched by a SELL). */
    @Query(value = "SELECT COALESCE(SUM(eur_amount), 0) FROM crypto_paper_order " +
                   "WHERE side = 'BUY' AND status = 'FILLED'",
           nativeQuery = true)
    BigDecimal sumOpenExposureEur();
}
