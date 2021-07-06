package nu.itark.frosk.dataset;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TestJDateManager {



    @Test
    public final void getDate() {
        String dateTime = "2021-04-22T12:22:08.153596Z";
        DateManager.getSeconds(dateTime);
        assertTrue(DateManager.getSeconds(dateTime) == 8);

        String dateTime1 = "2021-04-22T12:22:00.153596Z";
        DateManager.getSeconds(dateTime1);
        assertTrue(DateManager.getSeconds(dateTime1) == 0);


    }


    @Test
    public void testInstant() {
        DateTimeFormatter f = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.systemDefault());
        ZonedDateTime zdt = ZonedDateTime.parse("2014-09-02T08:05:23.653Z", f);
        int seconds = zdt.getSecond();
        System.out.println("seconds" + seconds);

    }



}
