package nu.itark.frosk.service;

import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.model.HedgeIndex;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class TestJHedgeIndexService extends BaseIntegrationTest {

    @Autowired
    private HedgeIndexService hedgeIndexService;

    @Test
    public void testUpdate() throws Exception {

        hedgeIndexService.update();

    }

}
