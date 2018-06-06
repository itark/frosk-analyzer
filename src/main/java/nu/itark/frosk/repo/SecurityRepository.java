package nu.itark.frosk.repo;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import nu.itark.frosk.model.Security;

public interface SecurityRepository extends CrudRepository<Security, Long>{
	List<Security> findByName(String name);
	List<Security> findByDatabase(String database);
}
