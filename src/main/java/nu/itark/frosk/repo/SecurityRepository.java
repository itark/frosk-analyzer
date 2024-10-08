package nu.itark.frosk.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import nu.itark.frosk.model.Security;

@Repository
public interface SecurityRepository extends JpaRepository<Security, Long>{
	Security findByName(String name);
	List<Security> findByDatabaseAndActive(String database, boolean active);
	List<Security> findByDatabaseAndActiveAndQuoteCurrency(String database, boolean active,String quoteCurrency);
	List<Security> findByDatabaseAndQuoteCurrency(String database, String quoteCurrency);
	List<Security> findByDatabase(String database);
	List<Security> findAllByActiveAndQuoteCurrency(boolean active, String quoteCurrency);
	List<Security> findAllByQuoteCurrency(String quoteCurrency);
	boolean existsByName(String name);
	
}
