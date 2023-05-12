package nu.itark.frosk.coinbase;

import nu.itark.frosk.FroskStartupApplicationListener;
import nu.itark.frosk.crypto.coinbase.advanced.Coinbase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

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
    public Coinbase exchange;

    @MockBean
    private FroskStartupApplicationListener applicationStartup;
}
