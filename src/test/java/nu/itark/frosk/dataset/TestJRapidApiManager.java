package nu.itark.frosk.dataset;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.crypto.coinbase.advanced.Coinbase;
import nu.itark.frosk.crypto.coinbase.api.products.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.IOException;

// https://rapidapi.com/sparior/api/yahoo-finance15/playground/apiendpoint_b7dd3888-f254-4081-a4cb-178d5638136e


@SpringBootTest
@Slf4j
public class TestJRapidApiManager {

    @MockBean
    Coinbase coinbase;

    @MockBean
    ProductService productService;


    @Autowired
    RapidApiManager rapidApiManager;

    @Test
    public void testGet() throws IOException, InterruptedException {

      //  rapidApiManager.get();

      //  rapidApiManager.get2();


        rapidApiManager.get3();  //Funkar

    }


}
