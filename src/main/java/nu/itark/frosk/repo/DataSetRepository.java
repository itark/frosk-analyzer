package nu.itark.frosk.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import nu.itark.frosk.model.DataSet;

public interface DataSetRepository extends JpaRepository<DataSet, Long>{
	DataSet findByName(String name);
}
