package nu.itark.frosk.repo;

import nu.itark.frosk.model.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountTypeRepository extends JpaRepository<AccountType, Long>{

    AccountType findByTypeAndInherentExitRule(String type, Boolean inherentExitRule);

}
