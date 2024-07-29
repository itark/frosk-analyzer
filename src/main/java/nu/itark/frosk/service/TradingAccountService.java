package nu.itark.frosk.service;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.analysis.TotalTradingDTO;
import nu.itark.frosk.model.AccountType;
import nu.itark.frosk.model.TradingAccount;
import nu.itark.frosk.repo.AccountTypeRepository;
import nu.itark.frosk.repo.TradingAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.ta4j.core.Position;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static java.math.RoundingMode.FLOOR;

@Component
@Slf4j
public class TradingAccountService {

    @Value("${frosk.inherent.exitrule:TRUE}")
    public Boolean inherentExitRule;

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

    private  TradingAccount activeTradingAccount;

    @Transactional
    public void initTradingAccounts() {
        accountTypeRepository.deleteAll();
        tradingAccountRepository.deleteAll();

        setAccountTypes();

        getAccountTypes().forEach(accountType-> {
            TradingAccount newTradingAccount = new TradingAccount();
            newTradingAccount.setCreateDate(new Date());
            newTradingAccount.setAccountType(accountType);
            newTradingAccount.setInitTotalValue(initTotalValue);
            newTradingAccount.setAccountValue(initTotalValue);
            newTradingAccount.setPositionValue(positionValue);
            tradingAccountRepository.save(newTradingAccount);
        });
        activeTradingAccount = getDefaultActiveTradingAccount();
    }

    private List<AccountType> getAccountTypes() {
        return accountTypeRepository.findAll();
    }

    private void setAccountTypes() {
        //inherentExitRule
        AccountType accountType = new AccountType();
        accountType.setCreateDate(new Date());
        accountType.setType(tradingAccountType);
        accountType.setInherentExitRule(inherentExitRule);
        accountTypeRepository.save(accountType);
        //!inherentExitRule
        accountType = new AccountType();
        accountType.setCreateDate(new Date());
        accountType.setType(tradingAccountType);
        accountType.setInherentExitRule(!inherentExitRule);
        accountTypeRepository.saveAndFlush(accountType);
    }

    public TradingAccount getDefaultActiveTradingAccount() {
       final AccountType accountType = accountTypeRepository.findByTypeAndInherentExitRule(tradingAccountType, inherentExitRule);
        return tradingAccountRepository.findByAccountType(accountType);
    }

    public void setActiveTradingAccount(TradingAccount activeTradingAccount) {
        this.activeTradingAccount = activeTradingAccount;
    }

    public TradingAccount getActiveTradingAccount() {
        return activeTradingAccount;
    }

    public void updateAccount(Position position) {
        if (Objects.isNull(activeTradingAccount)) {
            throw new RuntimeException("TradingAccount not initiated.");
        }

        setAccountValue(position.getProfit());
        //setSecurityValue(position);
        tradingAccountRepository.save(activeTradingAccount);

    }

    private void setSecurityValue(Position position) {
        Num netPrice = position.getEntry().getNetPrice();
        Num amount= position.getEntry().getAmount();
        Num entryPositionValue = netPrice.multipliedBy(amount);
        final Num currentSecurityValue = DoubleNum.valueOf(activeTradingAccount.getSecurityValue());
        if(position.getProfit().isPositive()) {
            final Num newSecurityValue = currentSecurityValue.plus(entryPositionValue);
            activeTradingAccount.setSecurityValue(BigDecimal.valueOf(newSecurityValue.doubleValue()));
        } else {
            final Num newSecurityValue = currentSecurityValue.minus(entryPositionValue.abs());
            activeTradingAccount.setSecurityValue(BigDecimal.valueOf(newSecurityValue.doubleValue()));
        }
    }

    private void setAccountValue(Num profit) {
        if (profit.isZero()) {
            return;
        }
        Num totValue = DoubleNum.valueOf(activeTradingAccount.getAccountValue());
        if(profit.isPositive()) {
            Num posPlus = totValue.plus(profit);
            activeTradingAccount.setAccountValue(BigDecimal.valueOf(posPlus.doubleValue()));
        } else {
            Num posNeg = totValue.minus(profit.abs());  //OBS. the abs()
            activeTradingAccount.setAccountValue(BigDecimal.valueOf(posNeg.doubleValue()));
        }

    }

    public BigDecimal getAccountValue() {
        return activeTradingAccount.getAccountValue();
    }

    public List<TotalTradingDTO> getTradingAccounts() {
        return tradingAccountRepository.findAll().stream()
                .map(ent -> convert(ent))
                .toList();
    }

    private TotalTradingDTO convert(TradingAccount tradingAccount) {
        return TotalTradingDTO.builder()
                .createDate(tradingAccount.getCreateDate())
                .initTotalValue(tradingAccount.getInitTotalValue())
                .positionValue(tradingAccount.getPositionValue())
                .securityValue(tradingAccount.getSecurityValue())
                .accountValue(tradingAccount.getAccountValue())
                .totalReturnPercentage(tradingAccount.getTotalReturnPercentage())
                .type(tradingAccount.getAccountType().getType())
                .inherentExitrule(String.valueOf(tradingAccount.getAccountType().getInherentExitRule()))
                .build();
    }


    public void updateTotalReturnPercentage() {
        activeTradingAccount.setTotalReturnPercentage(getTotalReturnPercentage(activeTradingAccount.getInitTotalValue(), activeTradingAccount.getAccountValue()));
        tradingAccountRepository.save(activeTradingAccount);
    }

    private BigDecimal getTotalReturnPercentage(BigDecimal initValue, BigDecimal accountValue) {
        return  ((accountValue.subtract(initValue))
                .divide(initValue, 4, FLOOR))
                .multiply(BigDecimal.valueOf(100L));
    }

}
