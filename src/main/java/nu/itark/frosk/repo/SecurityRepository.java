package nu.itark.frosk.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import nu.itark.frosk.model.Security;

@Repository
public interface SecurityRepository extends JpaRepository<Security, Long>{
	Security findByName(String name);
	Optional<Security> findById(Long id);
	List<Security> findByDatabase(String database);
	boolean existsByName(String name);
	
}
