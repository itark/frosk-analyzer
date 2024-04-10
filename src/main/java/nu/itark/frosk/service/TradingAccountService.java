package nu.itark.frosk.service;

import nu.itark.frosk.model.AccountType;
import nu.itark.frosk.model.TradingAccount;
import nu.itark.frosk.model.dto.AccountTypeDTO;
import nu.itark.frosk.repo.AccountTypeRepository;
import nu.itark.frosk.repo.TradingAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;

@Component
public class TradingAccountService {

    @Value("${frosk.init.total.value}")
    private BigDecimal initTotalValue;

    @Value("${frosk.position.value}")
    private BigDecimal positionValue;

    @Value("${frosk.tradingaccount.type:SANDBOX}")
    private String tradingAccountType;

    @Autowired
    TradingAccountRepository tradingAccountRepository;

    @Autowired
    AccountTypeRepository accountTypeRepository;

    private  TradingAccount tradingAccount;

    public void initTradingAccount() {
        final AccountType accountType = getAccountType();
        tradingAccount = getTradingAccount();
        if (Objects.nonNull(tradingAccount)) {
            return;
        }
        TradingAccount newTradingAccount = new TradingAccount();
        newTradingAccount.setCreateDate(new Date());
        newTradingAccount.setType(accountType.getType());
        newTradingAccount.setInitTotalValue(initTotalValue);
        newTradingAccount.setPositionValue(positionValue);
        tradingAccount = tradingAccountRepository.save(newTradingAccount);
    }

    private AccountType getAccountType() {
        if (accountTypeRepository.findAll().isEmpty()) {
            AccountType accountType = new AccountType();
            accountType.setCreateDate(new Date());
            accountType.setType(AccountTypeDTO.valueOf(tradingAccountType));
            return accountTypeRepository.save(accountType);
        } else {
            return (accountTypeRepository.findAll().get(0));
        }
    }

    public TradingAccount getTradingAccount() {
        final AccountType accountType = accountTypeRepository.findAll().get(0); //should only be one.
        return tradingAccountRepository.findByType(accountType.getType());
    }

    public void updateAccountOnProfit(Num profit) {
        if (Objects.isNull(tradingAccount)) {
            throw new RuntimeException("TradingAccount not initiated.");
        }
        if (profit.isZero()) {
            return;
        }
        Num totValue = DoubleNum.valueOf(tradingAccount.getTotalValue());
        if(profit.isPositive()) {
            Num posPlus = totValue.plus(profit);
            tradingAccount.setTotalValue(BigDecimal.valueOf(posPlus.doubleValue()));
        } else {
            Num posNeg = totValue.minus(profit.abs());  //OBS. the abs()
            tradingAccount.setTotalValue(BigDecimal.valueOf(posNeg.doubleValue()));
        }
        tradingAccountRepository.save(tradingAccount);
    }

    public BigDecimal getTotalValue() {
        //reload
        tradingAccount = tradingAccountRepository.findByType(tradingAccount.getType());
        return tradingAccount.getTotalValue();
    }

    public void deleteTotalValue() {
        tradingAccount = tradingAccountRepository.findByType(tradingAccount.getType());
        tradingAccount.setTotalValue(BigDecimal.ZERO);
        tradingAccountRepository.save(tradingAccount);
    }


}
