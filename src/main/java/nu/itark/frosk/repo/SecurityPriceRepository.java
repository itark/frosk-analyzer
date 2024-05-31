package nu.itark.frosk.repo;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import nu.itark.frosk.model.SecurityPrice;
@Repository
public interface SecurityPriceRepository extends JpaRepository<SecurityPrice, Long>{
	List<SecurityPrice> findBySecurityIdOrderByTimestamp(long securityId);
	SecurityPrice findTopBySecurityIdOrderByTimestampDesc(long id);
	SecurityPrice findBySecurityIdAndTimestamp(long securityId, Date timestamp);

}
