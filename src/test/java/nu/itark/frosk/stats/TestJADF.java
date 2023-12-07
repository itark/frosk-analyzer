package nu.itark.frosk.stats;

import nu.itark.frosk.strategies.stats.ADF;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestJADF  {


    @Test
    public void testStationary() {
        double[] ts = {1.20012, 1.20012, 1.20012,1.20012,1.20012,1.20012,1.20012,1.20012,1.20012,1.20012,1.20012,1.20012,1.20012,1.20012,1.20012,1.20012,1.20012,1.20012};

        ADF adf = new ADF(ts);

        System.out.println("adf.isStationary()"+adf.isStationary());

        assertTrue(adf.isStationary());

    }
}
