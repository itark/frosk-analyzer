package nu.itark.frosk.service;

import nu.itark.frosk.forecast.LstmServiceClient;
import nu.itark.frosk.forecast.LstmSignalResponse;
import org.junit.jupiter.api.Test;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Plain unit test, no Spring context — LstmSignalFilterService is a pure
 * constructor-injected POJO service, same style as TestJRegimeForecastService.
 */
public class TestJLstmSignalFilterService {

    private static final double THRESHOLD = 0.55;

    private BarSeries seriesWithBars(int count) {
        BarSeries series = new BaseBarSeriesBuilder().withName("TEST-ETH-EUR").build();
        ZonedDateTime t = ZonedDateTime.now().minusMinutes(15L * (count + 5));
        double price = 100.0;
        for (int i = 0; i < count; i++) {
            t = t.plusMinutes(15);
            price += 0.1;
            series.addBar(Duration.ofMinutes(15), t, price, price + 0.2, price - 0.2, price, 1000.0);
        }
        return series;
    }

    @Test
    public void disabled_neverCallsClient_andReturnsFalse() {
        LstmServiceClient client = mock(LstmServiceClient.class);
        LstmSignalFilterService service = new LstmSignalFilterService(client, false, THRESHOLD);

        BarSeries series = seriesWithBars(30);
        boolean result = service.isBuySignal(series, series.getEndIndex());

        assertFalse(result);
        verifyNoInteractions(client);
        assertFalse(service.isEnabled());
    }

    @Test
    public void tooFewBars_neverCallsClient_returnsFalse() {
        LstmServiceClient client = mock(LstmServiceClient.class);
        LstmSignalFilterService service = new LstmSignalFilterService(client, true, THRESHOLD);

        BarSeries series = seriesWithBars(10); // fewer than the 25-bar window
        boolean result = service.isBuySignal(series, series.getEndIndex());

        assertFalse(result);
        verifyNoInteractions(client);
    }

    @Test
    public void clientUnavailable_failsClosedToFalse() {
        LstmServiceClient client = mock(LstmServiceClient.class);
        when(client.fetchSignal(anyString(), any())).thenReturn(Optional.empty());
        LstmSignalFilterService service = new LstmSignalFilterService(client, true, THRESHOLD);

        BarSeries series = seriesWithBars(30);
        boolean result = service.isBuySignal(series, series.getEndIndex());

        assertFalse(result);
    }

    @Test
    public void fallbackResponse_treatedAsNotABuySignal_evenIfPredictionSaysBuy() {
        LstmServiceClient client = mock(LstmServiceClient.class);
        when(client.fetchSignal(anyString(), any()))
                .thenReturn(Optional.of(new LstmSignalResponse(0.9, "BUY", true)));
        LstmSignalFilterService service = new LstmSignalFilterService(client, true, THRESHOLD);

        BarSeries series = seriesWithBars(30);
        boolean result = service.isBuySignal(series, series.getEndIndex());

        assertFalse(result);
    }

    @Test
    public void buyPredictionAboveThreshold_isABuySignal() {
        LstmServiceClient client = mock(LstmServiceClient.class);
        when(client.fetchSignal(anyString(), any()))
                .thenReturn(Optional.of(new LstmSignalResponse(0.72, "BUY", false)));
        LstmSignalFilterService service = new LstmSignalFilterService(client, true, THRESHOLD);

        BarSeries series = seriesWithBars(30);
        boolean result = service.isBuySignal(series, series.getEndIndex());

        assertTrue(result);
    }

    @Test
    public void buyPredictionBelowThreshold_isNotABuySignal() {
        LstmServiceClient client = mock(LstmServiceClient.class);
        when(client.fetchSignal(anyString(), any()))
                .thenReturn(Optional.of(new LstmSignalResponse(0.40, "BUY", false)));
        LstmSignalFilterService service = new LstmSignalFilterService(client, true, THRESHOLD);

        BarSeries series = seriesWithBars(30);
        boolean result = service.isBuySignal(series, series.getEndIndex());

        assertFalse(result);
    }

    @Test
    public void neutralPrediction_isNotABuySignal() {
        LstmServiceClient client = mock(LstmServiceClient.class);
        when(client.fetchSignal(anyString(), any()))
                .thenReturn(Optional.of(new LstmSignalResponse(0.80, "NEUTRAL", false)));
        LstmSignalFilterService service = new LstmSignalFilterService(client, true, THRESHOLD);

        BarSeries series = seriesWithBars(30);
        boolean result = service.isBuySignal(series, series.getEndIndex());

        assertFalse(result);
    }

    @Test
    public void resultIsCached_secondCallDoesNotHitClientAgain() {
        LstmServiceClient client = mock(LstmServiceClient.class);
        when(client.fetchSignal(anyString(), any()))
                .thenReturn(Optional.of(new LstmSignalResponse(0.72, "BUY", false)));
        LstmSignalFilterService service = new LstmSignalFilterService(client, true, THRESHOLD);

        BarSeries series = seriesWithBars(30);
        int index = series.getEndIndex();

        boolean first = service.isBuySignal(series, index);
        boolean second = service.isBuySignal(series, index);

        assertEquals(first, second);
        verify(client, times(1)).fetchSignal(anyString(), any());
    }
}
