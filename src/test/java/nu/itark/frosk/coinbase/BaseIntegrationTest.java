package nu.itark.frosk.coinbase;

import jakarta.annotation.PostConstruct;
import nu.itark.frosk.FroskApplication;
import nu.itark.frosk.FroskStartupApplicationListener;
import nu.itark.frosk.crypto.coinbase.advanced.Coinbase;
import nu.itark.frosk.service.TradingAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * This is an integration test. Tests extending this class should be
 * run it against the Coinbase Pro sandbox API.
 */

@SpringBootTest(classes = {FroskApplication.class})
@ActiveProfiles("prod")
public abstract class BaseIntegrationTest {

    @Autowired
    public Coinbase exchange;

    @Autowired
    TradingAccountService tradingAccountService;

    @MockBean
    private FroskStartupApplicationListener applicationStartup;


    @PostConstruct
    void initTradingingAccount() {
        tradingAccountService.initTradingAccounts();
    }

}
