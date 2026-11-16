package nu.itark.frosk.service;

import org.junit.jupiter.api.Test;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.num.DoubleNum;

import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit tests for the pure factor-screening helpers in {@link EquityFactorScreeningService} (S1/S2). */
public class TestJEquityFactorScreening {

    private static final double EPS = 1e-9;

    @Test
    public void zscoresStandardizeToMeanZeroUnitStd() {
        double[] z = EquityFactorScreeningService.zscores(new double[]{1, 2, 3, 4, 5}, 3.0);
        // mean 3, population std = sqrt(2) ~ 1.41421356
        assertEquals(0.0, z[2], 1e-6, "middle value sits at the mean");
        assertEquals(-z[4], z[0], 1e-6, "symmetric around the mean");
        assertEquals(-1.4142135, z[0], 1e-6);
        assertEquals(1.4142135, z[4], 1e-6);
    }

    @Test
    public void zscoresAreClampedToTheCap() {
        double[] z = EquityFactorScreeningService.zscores(new double[]{0, 0, 0, 0, 100}, 1.5);
        for (double v : z) {
            assertTrue(v >= -1.5 && v <= 1.5, "z clamped to +/-1.5, was " + v);
        }
        assertEquals(1.5, z[4], EPS, "the outlier is clamped to the cap");
    }

    @Test
    public void zscoresZeroWhenNoDispersion() {
        double[] z = EquityFactorScreeningService.zscores(new double[]{5, 5, 5}, 3.0);
        assertEquals(0.0, z[0], EPS);
        assertEquals(0.0, z[1], EPS);
        assertEquals(0.0, z[2], EPS);
    }

    @Test
    public void medianOddAndEven() {
        assertEquals(2.0, EquityFactorScreeningService.median(new double[]{3, 1, 2}), EPS);
        assertEquals(2.5, EquityFactorScreeningService.median(new double[]{4, 1, 3, 2}), EPS);
    }

    @Test
    public void momentumIsRocOverLookback() {
        // 5 bars; ROC(4) at the last bar = (200-100)/100*100 = 100%.
        BarSeries s = series(100, 110, 120, 130, 200);
        assertEquals(100.0, EquityFactorScreeningService.momentum(s, 4), 1e-6);
    }

    @Test
    public void realizedVolIsZeroForFlatPricesPositiveForMoves() {
        assertEquals(0.0, EquityFactorScreeningService.realizedVol(series(100, 100, 100, 100), 3), EPS);
        assertTrue(EquityFactorScreeningService.realizedVol(series(100, 110, 100, 115), 3) > 0,
                "a moving series has positive realized vol");
    }

    private static BarSeries series(double... closes) {
        BarSeries s = new BaseBarSeriesBuilder().withName("T").withNumTypeOf(DoubleNum.class).build();
        ZonedDateTime t = ZonedDateTime.parse("2020-01-01T00:00Z");
        for (int i = 0; i < closes.length; i++) {
            double c = closes[i];
            s.addBar(t.plusDays(i), c, c, c, c, 1d);
        }
        return s;
    }
}
