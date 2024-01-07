package nu.itark.frosk.coinbase;

import nu.itark.frosk.FroskApplication;
import nu.itark.frosk.FroskStartupApplicationListener;
import nu.itark.frosk.H2TestProfileJPAConfig;
import nu.itark.frosk.bot.bot.test.util.junit.BaseTest;
import nu.itark.frosk.crypto.coinbase.advanced.Coinbase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

/**
 * This is an integration test. Tests extending this class should be
 * run it against the Coinbase Pro sandbox API.
 */
@SpringBootTest(properties = {"spring.profiles.active=test"}, classes = {
        FroskApplication.class})
public abstract class BaseIntegrationTest extends BaseTest {

    @Autowired
    public Coinbase exchange;

    @MockBean
    private FroskStartupApplicationListener applicationStartup;
}
