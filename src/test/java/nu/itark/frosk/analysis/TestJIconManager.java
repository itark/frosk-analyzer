package nu.itark.frosk.analysis;

import nu.itark.frosk.FroskApplication;
import nu.itark.frosk.coinbase.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestJIconManager {
    @Test
    public void testIconUrl() {
        String url = IconManager.getIconUrl("BTC-EUR");
        assertEquals("https://cryptoicons.org/api/icon/btc/20",url );
    }

}
