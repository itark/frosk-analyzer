package nu.itark.frosk.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import nu.itark.frosk.model.DataSet;

@Repository
public interface DataSetRepository extends JpaRepository<DataSet, Long>{
	DataSet findByName(String name);
}
