package nu.itark.frosk.repo;

import nu.itark.frosk.model.DataSet;
import nu.itark.frosk.model.StrategyTrade;
import nu.itark.frosk.model.TradingAccount;
import nu.itark.frosk.model.dto.AccountTypeDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TradingAccountRepository extends JpaRepository<TradingAccount, Long>{

    TradingAccount findByType(AccountTypeDTO type);

    }
