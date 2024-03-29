package nu.itark.frosk.repo.coinbase;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.crypto.coinbase.api.marketdata.MarketData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * Created by ren7881 on 20/03/2017.
 */
@Slf4j
public class OrderItemDeserializerTest {

    private ObjectMapper mapper;

    @BeforeEach
    public void setUp() throws Exception {
        mapper = new ObjectMapper();
//        SimpleModule module = new SimpleModule();
//        module.addDeserializer(OrderItem.class, new OrderItemDeserializer());
//        mapper.registerModule(module);

    }

    /**
     * This is now out of date - the api delivers a list of strings - amount, price, order Id. NOT number of orders as this test shows.
     * @throws IOException
     */
    @Test
    public void testDesirialization() throws IOException {
        String test = "{\n" +
                "    \"sequence\": \"3\",\n" +
                "    \"bids\": [\n" +
                "        [ \"111.96\", \"2.11111\", 3 ],\n" +
                "        [ \"295.96\", \"4.39088265\", 2 ]\n" +
                "    ],\n" +
                "    \"asks\": [\n" +
                "        [ \"555.97\", \"66.5656565\", 10 ],\n" +
                "        [ \"295.97\", \"25.23542881\", 12 ]\n" +
                "    ]\n" +
                "}";

        MarketData marketData = mapper.readValue(test, MarketData.class);
        
        log.info("test="+test);
        
        
        
        log.info("marketData="+marketData);
        
        
    }
}