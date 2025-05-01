package nu.itark.frosk.repo;

import nu.itark.frosk.model.HedgeIndex;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface HedgeIndexRepository extends JpaRepository<HedgeIndex, Long>{

    List<HedgeIndex> findByDate(Date date);

    Optional<HedgeIndex> findTopByIndicatorOrderByDateDesc(@Param("indicator") String indicator);

    List<HedgeIndex> findByIndicator(String indicator);

}
