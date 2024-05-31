package nu.itark.frosk.repo;

import nu.itark.frosk.model.AccountType;
import nu.itark.frosk.model.TradingAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TradingAccountRepository extends JpaRepository<TradingAccount, Long>{

    TradingAccount findByAccountType(AccountType type);

    }
