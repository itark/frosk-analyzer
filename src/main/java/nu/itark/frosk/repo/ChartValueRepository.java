package nu.itark.frosk.repo;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import nu.itark.frosk.model.ChartValue;

public interface ChartValueRepository extends CrudRepository<ChartValue, Long>{
	List<ChartValue> findBySecurity(String security);
	ChartValue findTopBySecurityOrderByTimestampDesc(String security);
	
}
