package nu.itark.frosk.service;

import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.repo.StrategyTradeRepository;
import nu.itark.frosk.repo.TradingAccountRepository;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Position;
import org.ta4j.core.num.Num;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class TestJTradingAccountService extends BaseIntegrationTest {

    @Autowired
    TradingAccountRepository tradingAccountRepository;

    @Autowired
    private TradingAccountService tradingAccountService;


    @Test
    public void testInitAccounts() throws Exception {
        tradingAccountService.initTradingAccounts();

    }


}
