package nu.itark.frosk.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import nu.itark.frosk.model.DataSet;
import nu.itark.frosk.model.Security;

public interface DataSetRepository extends JpaRepository<DataSet, Long>{
	DataSet findByName(String name);
}
