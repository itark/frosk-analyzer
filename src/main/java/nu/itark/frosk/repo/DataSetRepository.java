package nu.itark.frosk.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import nu.itark.frosk.model.DataSet;

public interface DataSetRepository extends JpaRepository<DataSet, Long>{
	DataSet findByName(String name);
//	List<DataSet> findByName(String name);
}
