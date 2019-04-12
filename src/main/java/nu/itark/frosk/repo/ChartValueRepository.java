package nu.itark.frosk.repo;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import nu.itark.frosk.model.ChartValue;

@Repository
public interface ChartValueRepository extends CrudRepository<ChartValue, Long>{
	List<ChartValue> findBySecurity(String security);
	ChartValue findTopBySecurityOrderByTimestampDesc(String security);
	
}
