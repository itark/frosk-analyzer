package nu.itark.frosk.repo;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import nu.itark.frosk.model.SecurityPrice;

public interface SecurityPriceRepository extends CrudRepository<SecurityPrice, Long>{
	List<SecurityPrice> findByName(String name);
	SecurityPrice findTopByNameOrderByTimestampDesc(String name);
}
