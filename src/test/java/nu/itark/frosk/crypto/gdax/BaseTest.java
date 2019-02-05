package nu.itark.frosk.crypto.gdax;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import nu.itark.frosk.FroskApplication;
import nu.itark.frosk.coinbase.exchange.api.exchange.GdaxExchange;

/**
 * Created by robevansuk on 20/01/2017.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
                 FroskApplication.class
                },
                properties = {
                    "spring.profiles.active=test"
                }
)
public abstract class BaseTest {

    @Autowired
    public GdaxExchange exchange;
}
