package nu.itark.frosk.coinbase;

import com.coinbase.exchange.api.exchange.CoinbaseExchange;
import nu.itark.frosk.FroskStartupApplicationListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is an integration test. Tests extending this class should be
 * run it against the Coinbase Pro sandbox API. To do this you will need
 * to provide credentials in resources/application-test.yml
 *
 * Created by robevansuk on 20/01/2017.
 */
@SpringBootTest(properties = {
                    "spring.profiles.active=test"
                })
public abstract class BaseIntegrationTest {

    @Autowired
    public CoinbaseExchange exchange;

    @MockBean
    private FroskStartupApplicationListener applicationStartup;
}
