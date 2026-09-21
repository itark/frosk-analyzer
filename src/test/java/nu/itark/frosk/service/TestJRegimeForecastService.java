package nu.itark.frosk.service;

import nu.itark.frosk.regime.GarchServiceClient;
import nu.itark.frosk.regime.Regime;
import nu.itark.frosk.regime.VolatilityRegimeResponse;
import org.junit.jupiter.api.Test;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Plain unit test, no Spring context — RegimeForecastService is a pure
 * constructor-injected POJO service. Focused on the fail-closed and
 * disabled-means-no-HTTP-call contracts documented on the class.
 */
public class TestJRegimeForecastService {

    private static final int MIN_OBSERVATIONS = 200;
    private static final int BARS = 300;

    /** Trending series: steadily rising close, strong ADX. */
    private BarSeries trendingSeries() {
        BarSeries series = new BaseBarSeriesBuilder().withName("TEST-TRENDING").build();
        ZonedDateTime t = ZonedDateTime.now().minusDays(BARS + 5);
        double price = 100.0;
        for (int i = 0; i < BARS; i++) {
            t = t.plusDays(1);
            price += 1.0; // strong, steady uptrend -> high ADX
            series.addBar(Duration.ofDays(1), t, price, price + 0.5, price - 0.5, price, 1000.0);
        }
        return series;
    }

    /** Flat/choppy series: no sustained direction, weak ADX. */
    private BarSeries sidewaysSeries() {
        BarSeries series = new BaseBarSeriesBuilder().withName("TEST-SIDEWAYS").build();
        ZonedDateTime t = ZonedDateTime.now().minusDays(BARS + 5);
        for (int i = 0; i < BARS; i++) {
            t = t.plusDays(1);
            double price = 100.0 + (i % 2 == 0 ? 0.3 : -0.3); // oscillates around 100, no trend
            series.addBar(Duration.ofDays(1), t, price, price + 0.1, price - 0.1, price, 1000.0);
        }
        return series;
    }

    private VolatilityRegimeResponse response(String volatilityRegime, boolean converged) {
        return new VolatilityRegimeResponse(0.01, 50.0, volatilityRegime, converged, MIN_OBSERVATIONS + 50, "GARCH(1,1)");
    }

    @Test
    public void disabled_neverCallsClient_andReturnsUnknown() {
        GarchServiceClient client = mock(GarchServiceClient.class);
        RegimeForecastService service = new RegimeForecastService(client, false, MIN_OBSERVATIONS, 1000, 25.0);

        BarSeries series = trendingSeries();
        Regime regime = service.getRegime(series, series.getEndIndex());

        assertEquals(Regime.UNKNOWN, regime);
        verifyNoInteractions(client);
        assertEquals(false, service.isEnabled());
    }

    @Test
    public void tooFewObservations_neverCallsClient_returnsUnknown() {
        GarchServiceClient client = mock(GarchServiceClient.class);
        RegimeForecastService service = new RegimeForecastService(client, true, MIN_OBSERVATIONS, 1000, 25.0);

        // Only 50 bars total -> far fewer returns than MIN_OBSERVATIONS.
        BarSeries series = new BaseBarSeriesBuilder().withName("TOO-SHORT").build();
        ZonedDateTime t = ZonedDateTime.now().minusDays(55);
        for (int i = 0; i < 50; i++) {
            t = t.plusDays(1);
            series.addBar(Duration.ofDays(1), t, 100.0, 100.5, 99.5, 100.0, 1000.0);
        }

        Regime regime = service.getRegime(series, series.getEndIndex());

        assertEquals(Regime.UNKNOWN, regime);
        verifyNoInteractions(client);
    }

    @Test
    public void clientUnavailable_failsClosedToUnknown() {
        GarchServiceClient client = mock(GarchServiceClient.class);
        when(client.fetchVolatilityRegime(anyString(), any())).thenReturn(Optional.empty());
        RegimeForecastService service = new RegimeForecastService(client, true, MIN_OBSERVATIONS, 1000, 25.0);

        Regime regime = service.getRegime(trendingSeries(), BARS - 1);

        assertEquals(Regime.UNKNOWN, regime);
    }

    @Test
    public void nonConvergedGarchFit_failsClosedToUnknown() {
        GarchServiceClient client = mock(GarchServiceClient.class);
        when(client.fetchVolatilityRegime(anyString(), any()))
                .thenReturn(Optional.of(response("UNKNOWN", false)));
        RegimeForecastService service = new RegimeForecastService(client, true, MIN_OBSERVATIONS, 1000, 25.0);

        Regime regime = service.getRegime(trendingSeries(), BARS - 1);

        assertEquals(Regime.UNKNOWN, regime);
    }

    @Test
    public void highVolatility_mapsToVolatile_regardlessOfTrend() {
        GarchServiceClient client = mock(GarchServiceClient.class);
        when(client.fetchVolatilityRegime(anyString(), any()))
                .thenReturn(Optional.of(response("HIGH", true)));
        RegimeForecastService service = new RegimeForecastService(client, true, MIN_OBSERVATIONS, 1000, 25.0);

        // Trending series would otherwise classify TRENDING — HIGH volatility must override it.
        Regime regime = service.getRegime(trendingSeries(), BARS - 1);

        assertEquals(Regime.VOLATILE, regime);
    }

    @Test
    public void normalVolatility_withStrongAdx_mapsToTrending() {
        GarchServiceClient client = mock(GarchServiceClient.class);
        when(client.fetchVolatilityRegime(anyString(), any()))
                .thenReturn(Optional.of(response("NORMAL", true)));
        RegimeForecastService service = new RegimeForecastService(client, true, MIN_OBSERVATIONS, 1000, 25.0);

        Regime regime = service.getRegime(trendingSeries(), BARS - 1);

        assertEquals(Regime.TRENDING, regime);
    }

    @Test
    public void normalVolatility_withWeakAdx_mapsToSideways() {
        GarchServiceClient client = mock(GarchServiceClient.class);
        when(client.fetchVolatilityRegime(anyString(), any()))
                .thenReturn(Optional.of(response("LOW", true)));
        RegimeForecastService service = new RegimeForecastService(client, true, MIN_OBSERVATIONS, 1000, 25.0);

        Regime regime = service.getRegime(sidewaysSeries(), BARS - 1);

        assertEquals(Regime.SIDEWAYS, regime);
    }

    @Test
    public void resultIsCached_secondCallDoesNotHitClientAgain() {
        GarchServiceClient client = mock(GarchServiceClient.class);
        when(client.fetchVolatilityRegime(anyString(), any()))
                .thenReturn(Optional.of(response("NORMAL", true)));
        RegimeForecastService service = new RegimeForecastService(client, true, MIN_OBSERVATIONS, 1000, 25.0);

        BarSeries series = trendingSeries();
        int index = series.getEndIndex();

        Regime first = service.getRegime(series, index);
        Regime second = service.getRegime(series, index);

        assertEquals(first, second);
        org.mockito.Mockito.verify(client, org.mockito.Mockito.times(1))
                .fetchVolatilityRegime(anyString(), any());
    }
}
