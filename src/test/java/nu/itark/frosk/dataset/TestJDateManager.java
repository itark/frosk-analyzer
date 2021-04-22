package nu.itark.frosk.dataset;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TestJDateManager {



    @Test
    public final void getDate() {
        String dateTime1 = "2021-04-22T12:22:08.153596Z";
        DateManager.get(dateTime1);

    }


    @Test
    public void testInstant() {

        // option a - parsed from the string
//    DateTimeFormatter f = DateTimeFormatter.ISO_DATE_TIME;
//    ZonedDateTime zdt = ZonedDateTime.parse("2014-09-02T08:05:23.653Z", f);

//    // option b - specified in the formatter - REQUIRES JDK 8u20 !!!
    DateTimeFormatter f = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.systemDefault());
    ZonedDateTime zdt = ZonedDateTime.parse("2014-09-02T08:05:23.653Z", f);


    }



}
