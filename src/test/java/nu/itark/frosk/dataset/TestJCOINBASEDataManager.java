package nu.itark.frosk.dataset;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.coinbase.BaseIntegrationTest;
import nu.itark.frosk.coinbase.config.IntegrationTestConfiguration;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;

@ExtendWith(SpringExtension.class)
//@Import({IntegrationTestConfiguration.class})
@org.junit.runner.RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class TestJCOINBASEDataManager extends BaseIntegrationTest {

    @Autowired
    COINBASEDataManager coinbaseDataManager;

    @Test
    public void syncOne(){
        coinbaseDataManager.syncronize("BTC-EUR");
    }

    @Test
    public void testInstansDate() {

       // ZonedDateTime date = ZonedDateTime.now();

        Instant date = Instant.now();

        // prints the date
        System.out.println(date);

        // Parses the date
        LocalDate date1 = LocalDate.parse("2015-01-31");

        // Uses the function to adjust the date
        date = (Instant)date1.adjustInto(date);

        // Prints the adjusted date
        System.out.println(date);


    }



}
