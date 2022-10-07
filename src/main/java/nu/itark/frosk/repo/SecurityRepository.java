package nu.itark.frosk.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import nu.itark.frosk.model.Security;

@Repository
public interface SecurityRepository extends JpaRepository<Security, Long>{
	Security findByName(String name);
	List<Security> findByDatabase(String database);
	boolean existsByName(String name);
	
}
