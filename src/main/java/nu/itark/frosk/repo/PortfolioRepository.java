package nu.itark.frosk.repo;

import nu.itark.frosk.model.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    Optional<Portfolio> findTopByOrderBySnapshotDateDesc();
}
