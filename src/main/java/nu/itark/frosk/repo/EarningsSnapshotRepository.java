package nu.itark.frosk.repo;

import nu.itark.frosk.model.EarningsSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface EarningsSnapshotRepository extends JpaRepository<EarningsSnapshot, Long> {

    boolean existsBySecurityIdAndCapturedOn(Long securityId, LocalDate capturedOn);

    List<EarningsSnapshot> findByTickerOrderByCapturedAtAsc(String ticker);

    List<EarningsSnapshot> findByAnnouncementDateOrderByTicker(LocalDate announcementDate);
}
