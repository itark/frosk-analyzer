package nu.itark.frosk.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import nu.itark.frosk.model.SecurityPrice;

public interface SecurityPriceRepository extends JpaRepository<SecurityPrice, Long>{
	List<SecurityPrice> findBySecurityIdOrderByTimestamp(long securityId);
	SecurityPrice findTopBySecurityIdOrderByTimestampDesc(long id);
}
